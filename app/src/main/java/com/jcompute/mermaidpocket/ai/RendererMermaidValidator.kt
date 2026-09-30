package com.jcompute.mermaidpocket.ai

import com.jcompute.mermaidpocket.render.MermaidRendererController

class RendererMermaidValidator(
    private val renderer: MermaidRendererController,
    private val themeId: () -> String,
) : MermaidValidator {
    override suspend fun validate(source: String): MermaidValidation {
        val result = renderer.parse(source, themeId())
        val error = result.exceptionOrNull()?.message.orEmpty()
        return MermaidValidation(ok = result.isSuccess, error = error)
    }
}
