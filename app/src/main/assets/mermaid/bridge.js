/* global mermaid */
(function () {
  "use strict";

  const emptyEl = document.getElementById("empty");
  const errorEl = document.getElementById("error");
  const stageEl = document.getElementById("stage");
  const viewportEl = document.getElementById("viewport");

  const MIN_ZOOM = 0.5; // keep in sync with DiagramDisplay.MIN_ZOOM
  const MAX_ZOOM = 5; // keep in sync with DiagramDisplay.MAX_ZOOM

  // mermaid.live pretty ER (Mermaid 12): rainbow entity strokes live only on
  // redux-color / redux-dark-color. `dark` / `default` / `neutral` are mono.
  const COLOR_THEMES = { "redux-color": true, "redux-dark-color": true };

  let lastTheme = null;
  let renderSeq = 0;
  let lastOkSource = null;
  let lastOkTheme = null;
  let lastOkSvg = null;
  let viewScale = 1;
  let viewX = 0;
  let viewY = 0;

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
    const dark = String(theme || "").indexOf("dark") !== -1;
    document.body.classList.toggle("dark", dark);
    // Do not pass themeVariables (fontSize / darkMode): that merge washes
    // borderColorArray so ER entities collapse to a single accent.
    // Do not shrink er.entityPadding — mermaid.live uses 15 + neo look.
    mermaid.initialize({
      startOnLoad: false,
      theme: theme,
      look: "neo",
      layout: "elk",
      securityLevel: "strict",
      htmlLabels: false,
      flowchart: { htmlLabels: true, curve: "basis", padding: 8, useMaxWidth: true },
      sequence: { actorMargin: 32, messageMargin: 28, useMaxWidth: true },
      class: { useMaxWidth: true },
      state: { useMaxWidth: true },
      pie: { useMaxWidth: true },
      gantt: { useMaxWidth: true },
      er: { useMaxWidth: true },
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
      const attrW = parseFloat(svg.getAttribute("width"));
      const attrH = parseFloat(svg.getAttribute("height"));
      if (attrW && attrH && !String(svg.getAttribute("width") || "").includes("%")) {
        width = width || attrW;
        height = height || attrH;
      }
    }
    if (!width || !height) {
      const rect = svg.getBoundingClientRect();
      width = width || rect.width;
      height = height || rect.height;
    }
    return { width: Math.max(1, width), height: Math.max(1, height) };
  }

  function currentSvg() {
    return viewportEl.querySelector("svg");
  }

  function applyViewTransform() {
    const svg = currentSvg();
    if (!svg || !viewportEl) return;
    const scale = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, viewScale || 1));
    const size = measureSvg(svg);
    if (stageEl.clientWidth < 2) {
      window.requestAnimationFrame(applyViewTransform);
      return;
    }
    const cs = window.getComputedStyle(stageEl);
    const padX = (parseFloat(cs.paddingLeft) || 0) + (parseFloat(cs.paddingRight) || 0);
    const stageW = Math.max(1, stageEl.clientWidth - padX);
    const fit = stageW / size.width;
    const displayW = size.width * fit * scale;
    const displayH = size.height * fit * scale;
    svg.style.maxWidth = "none";
    svg.style.width = displayW + "px";
    svg.style.height = displayH + "px";
    svg.removeAttribute("width");
    svg.removeAttribute("height");
    viewportEl.style.transform =
      "translate(" + (viewX || 0) + "px, " + (viewY || 0) + "px)";
  }

  function resetViewTransform() {
    viewScale = 1;
    viewX = 0;
    viewY = 0;
    applyViewTransform();
  }

  window.setViewTransform = function (scale, x, y) {
    viewScale = scale;
    viewX = x;
    viewY = y;
    applyViewTransform();
  };

  window.resetViewTransform = resetViewTransform;

  function serializeSvg(svg) {
    if (!svg) return "";
    const clone = svg.cloneNode(true);
    clone.style.maxWidth = "";
    clone.style.width = "";
    clone.style.height = "";
    clone.style.transform = "";
    if (!clone.getAttribute("xmlns")) {
      clone.setAttribute("xmlns", "http://www.w3.org/2000/svg");
    }
    if (!clone.getAttribute("xmlns:xlink")) {
      clone.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
    }
    const size = measureSvg(svg);
    clone.setAttribute("width", String(size.width));
    clone.setAttribute("height", String(size.height));
    if (!clone.getAttribute("viewBox")) {
      clone.setAttribute("viewBox", "0 0 " + size.width + " " + size.height);
    }
    return new XMLSerializer().serializeToString(clone);
  }

  function svgToPngDataUrl(svg, scale, background) {
    return new Promise(function (resolve, reject) {
      const size = measureSvg(svg);
      const bake = Math.max(2, Number(scale) || 2);
      const w = Math.max(1, Math.round(size.width * bake));
      const h = Math.max(1, Math.round(size.height * bake));
      const clone = svg.cloneNode(true);
      clone.style.maxWidth = "none";
      clone.style.width = w + "px";
      clone.style.height = h + "px";
      clone.style.transform = "";
      clone.setAttribute("width", String(w));
      clone.setAttribute("height", String(h));
      if (!clone.getAttribute("xmlns")) {
        clone.setAttribute("xmlns", "http://www.w3.org/2000/svg");
      }
      if (!clone.getAttribute("xmlns:xlink")) {
        clone.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
      }
      const xml = new XMLSerializer().serializeToString(clone);
      if (!xml) {
        reject(new Error("No SVG to export"));
        return;
      }
      const img = new Image();
      img.onload = function () {
        try {
          const canvas = document.createElement("canvas");
          canvas.width = w;
          canvas.height = h;
          const ctx = canvas.getContext("2d");
          ctx.imageSmoothingEnabled = true;
          ctx.imageSmoothingQuality = "high";
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

  function presentSuccess(svgMarkup, seq) {
    if (seq !== renderSeq) return;
    viewportEl.innerHTML = svgMarkup || "";
    const svg = currentSvg();
    if (!svg) {
      show("error");
      errorEl.textContent = "Renderer returned no SVG.";
      notify(false, errorEl.textContent, 0, 0);
      return;
    }
    if (!svg.getAttribute("preserveAspectRatio")) {
      svg.setAttribute("preserveAspectRatio", "xMidYMid meet");
    }
    show("stage");
    resetViewTransform();
    window.requestAnimationFrame(applyViewTransform);
    const size = measureSvg(svg);
    notify(true, "", Math.round(size.width), Math.round(size.height));
  }

  window.renderDiagram = function (source, theme) {
    const seq = ++renderSeq;
    const resolved = COLOR_THEMES[theme] ? theme : "redux-color";
    try {
      configure(resolved);
    } catch (err) {
      show("error");
      errorEl.textContent = String(err && err.message ? err.message : err);
      notify(false, errorEl.textContent, 0, 0);
      return;
    }

    const text = (source || "").trim();
    if (!text) {
      lastOkSource = null;
      lastOkTheme = null;
      lastOkSvg = null;
      viewportEl.innerHTML = "";
      show("empty");
      notify(true, "", 0, 0);
      return;
    }

    // Re-open / re-paste of identical source must still notify. mermaid.render
    // can no-op or hang on a second pass of the same graph; replay the last SVG.
    if (text === lastOkSource && resolved === lastOkTheme && lastOkSvg) {
      presentSuccess(lastOkSvg, seq);
      return;
    }

    try {
      mermaid
        .render("mmd-" + Date.now() + "-" + seq, text)
        .then(function (result) {
          if (seq !== renderSeq) return;
          lastOkSource = text;
          lastOkTheme = resolved;
          lastOkSvg = result.svg || "";
          presentSuccess(lastOkSvg, seq);
        })
        .catch(function (err) {
          if (seq !== renderSeq) return;
          lastOkSource = null;
          lastOkTheme = null;
          lastOkSvg = null;
          viewportEl.innerHTML = "";
          const message =
            (err && (err.str || err.message)) || String(err) || "Could not render diagram.";
          errorEl.textContent = message;
          show("error");
          notify(false, message, 0, 0);
        });
    } catch (err) {
      if (seq !== renderSeq) return;
      const message = String(err && err.message ? err.message : err);
      errorEl.textContent = message;
      show("error");
      notify(false, message, 0, 0);
    }
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

  window.addEventListener("resize", function () {
    if (currentSvg()) applyViewTransform();
  });

  let pinchStartDist = 0;
  let pinchStartScale = 1;
  let panLast = null;
  let lastTapAt = 0;
  let lastTapX = 0;
  let lastTapY = 0;

  function touchPoints(event) {
    const points = [];
    for (let i = 0; i < event.touches.length; i++) {
      points.push({
        x: event.touches[i].clientX,
        y: event.touches[i].clientY,
      });
    }
    return points;
  }

  function touchDistance(a, b) {
    return Math.hypot(a.x - b.x, a.y - b.y);
  }

  function touchMid(a, b) {
    return { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 };
  }

  function onTouchStart(event) {
    if (!currentSvg()) return;
    event.preventDefault();
    const points = touchPoints(event);
    if (points.length >= 2) {
      pinchStartDist = touchDistance(points[0], points[1]);
      pinchStartScale = viewScale;
      panLast = touchMid(points[0], points[1]);
      lastTapAt = 0;
      return;
    }
    if (points.length === 1) {
      panLast = { x: points[0].x, y: points[0].y };
      const now = Date.now();
      if (
        now - lastTapAt < 280 &&
        Math.hypot(points[0].x - lastTapX, points[0].y - lastTapY) < 48
      ) {
        resetViewTransform();
        lastTapAt = 0;
        panLast = null;
      } else {
        lastTapAt = now;
        lastTapX = points[0].x;
        lastTapY = points[0].y;
      }
    }
  }

  function onTouchMove(event) {
    if (!currentSvg()) return;
    event.preventDefault();
    const points = touchPoints(event);
    if (points.length >= 2) {
      const dist = touchDistance(points[0], points[1]);
      if (pinchStartDist > 1) {
        viewScale = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, pinchStartScale * (dist / pinchStartDist)));
      }
      const mid = touchMid(points[0], points[1]);
      if (panLast) {
        viewX += mid.x - panLast.x;
        viewY += mid.y - panLast.y;
      }
      panLast = mid;
      applyViewTransform();
      return;
    }
    if (points.length === 1 && panLast) {
      viewX += points[0].x - panLast.x;
      viewY += points[0].y - panLast.y;
      panLast = { x: points[0].x, y: points[0].y };
      applyViewTransform();
    }
  }

  function onTouchEnd(event) {
    if (event.touches.length === 0) {
      panLast = null;
      pinchStartDist = 0;
      return;
    }
    if (event.touches.length === 1) {
      panLast = { x: event.touches[0].clientX, y: event.touches[0].clientY };
      pinchStartDist = 0;
      pinchStartScale = viewScale;
    }
  }

  stageEl.addEventListener("touchstart", onTouchStart, { passive: false });
  stageEl.addEventListener("touchmove", onTouchMove, { passive: false });
  stageEl.addEventListener("touchend", onTouchEnd, { passive: false });
  stageEl.addEventListener("touchcancel", onTouchEnd, { passive: false });

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
