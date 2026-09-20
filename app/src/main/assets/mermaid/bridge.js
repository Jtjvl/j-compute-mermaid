/* global mermaid */
(function () {
  "use strict";

  const emptyEl = document.getElementById("empty");
  const errorEl = document.getElementById("error");
  const stageEl = document.getElementById("stage");

  let lastTheme = null;
  let renderSeq = 0;

  function notify(ok, error, width, height) {
    try {
      if (window.MermaidBridge) {
        window.MermaidBridge.onRendered(!!ok, error || "", width | 0, height | 0);
      }
    } catch (e) {
      console.error(e);
    }
  }

  function notifyExport(kind, payload) {
    try {
      if (window.MermaidBridge) {
        window.MermaidBridge.onExport(kind, payload || "");
      }
    } catch (e) {
      console.error(e);
    }
  }

  function show(which) {
    emptyEl.style.display = which === "empty" ? "block" : "none";
    errorEl.style.display = which === "error" ? "block" : "none";
    stageEl.style.display = which === "stage" ? "flex" : "none";
  }

  function configure(theme) {
    if (typeof mermaid === "undefined") {
      throw new Error("mermaid.js failed to load from bundled assets.");
    }
    if (lastTheme === theme) return;
    lastTheme = theme;
    document.body.classList.toggle("dark", theme === "dark");
    mermaid.initialize({
      startOnLoad: false,
      theme: theme,
      securityLevel: "strict",
      fontFamily: "ui-sans-serif, system-ui, sans-serif",
      flowchart: { htmlLabels: true, curve: "basis", padding: 12 },
      sequence: { actorMargin: 40, messageMargin: 32 },
      class: { useMaxWidth: true },
    });
  }

  function measureSvg(svg) {
    if (!svg) return { width: 0, height: 0 };
    let width = 0;
    let height = 0;
    if (svg.viewBox && svg.viewBox.baseVal && svg.viewBox.baseVal.width) {
      width = svg.viewBox.baseVal.width;
      height = svg.viewBox.baseVal.height;
    }
    if (!width || !height) {
      const box = svg.getBBox ? svg.getBBox() : null;
      if (box) {
        width = width || box.width;
        height = height || box.height;
      }
    }
    if (!width || !height) {
      const rect = svg.getBoundingClientRect();
      width = width || rect.width;
      height = height || rect.height;
    }
    return { width: Math.round(width), height: Math.round(height) };
  }

  function currentSvg() {
    return stageEl.querySelector("svg");
  }

  function serializeSvg(svg) {
    if (!svg) return "";
    const clone = svg.cloneNode(true);
    if (!clone.getAttribute("xmlns")) {
      clone.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    }
    if (!clone.getAttribute("xmlns:xlink")) {
      clone.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
    }
    return new XMLSerializer().serializeToString(clone);
  }

  function svgToPngDataUrl(svg, scale, background) {
    return new Promise(function (resolve, reject) {
      const xml = serializeSvg(svg);
      if (!xml) {
        reject(new Error("No SVG to export"));
        return;
      }
      const size = measureSvg(svg);
      const img = new Image();
      img.onload = function () {
        try {
          const canvas = document.createElement("canvas");
          const w = Math.max(1, Math.round((img.width || size.width || 1) * scale));
          const h = Math.max(1, Math.round((img.height || size.height || 1) * scale));
          canvas.width = w;
          canvas.height = h;
          const ctx = canvas.getContext("2d");
          if (background) {
            ctx.fillStyle = background;
            ctx.fillRect(0, 0, w, h);
          }
          ctx.drawImage(img, 0, 0, w, h);
          resolve(canvas.toDataURL("image/png"));
        } catch (err) {
          reject(err);
        }
      };
      img.onerror = function () {
        reject(new Error("Could not rasterize SVG"));
      };
      img.src = "data:image/svg+xml;charset=utf-8," + encodeURIComponent(xml);
    });
  }

  window.renderDiagram = function (source, theme) {
    const seq = ++renderSeq;
    try {
      configure(theme || "default");
    } catch (err) {
      show("error");
      errorEl.textContent = String(err && err.message ? err.message : err);
      notify(false, errorEl.textContent, 0, 0);
      return;
    }

    const text = (source || "").trim();
    if (!text) {
      stageEl.innerHTML = "";
      show("empty");
      notify(true, "", 0, 0);
      return;
    }

    mermaid
      .render("mmd-" + Date.now() + "-" + seq, text)
      .then(function (result) {
        if (seq !== renderSeq) return;
        stageEl.innerHTML = result.svg || "";
        const svg = currentSvg();
        if (!svg) {
          show("error");
          errorEl.textContent = "Renderer returned no SVG.";
          notify(false, errorEl.textContent, 0, 0);
          return;
        }
        show("stage");
        const size = measureSvg(svg);
        notify(true, "", size.width, size.height);
      })
      .catch(function (err) {
        if (seq !== renderSeq) return;
        stageEl.innerHTML = "";
        const message =
          (err && (err.str || err.message)) || String(err) || "Could not render diagram.";
        errorEl.textContent = message;
        show("error");
        notify(false, message, 0, 0);
      });
  };

  window.exportSvg = function () {
    notifyExport("svg", serializeSvg(currentSvg()));
  };

  window.exportPng = function (scale, background) {
    const svg = currentSvg();
    if (!svg) {
      notifyExport("error", "Nothing to export. Render a diagram first.");
      return;
    }
    svgToPngDataUrl(svg, scale || 2, background || "#ffffff")
      .then(function (dataUrl) {
        notifyExport("png", dataUrl);
      })
      .catch(function (err) {
        notifyExport("error", String(err && err.message ? err.message : err));
      });
  };

  function ready() {
    try {
      if (window.MermaidBridge) {
        window.MermaidBridge.onReady();
      }
    } catch (e) {
      console.error(e);
    }
  }

  if (document.readyState === "complete" || document.readyState === "interactive") {
    ready();
  } else {
    document.addEventListener("DOMContentLoaded", ready);
  }
})();
