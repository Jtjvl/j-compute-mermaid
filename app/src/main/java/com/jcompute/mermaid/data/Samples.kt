package com.jcompute.mermaid.data

data class SampleDiagram(
    val name: String,
    val source: String,
)

object Samples {
    val flowchart = SampleDiagram(
        name = "Flowchart",
        source = """
            %% Checkout
            flowchart TD
                A[Write Mermaid] --> B{Looks right?}
                B -->|Yes| C[Export or share]
                B -->|Tweak it| A
                C --> D([Done])
        """.trimIndent(),
    )

    val mermaidLiveClassic = SampleDiagram(
        name = "Classic flowchart",
        source = """
            flowchart TD
                A[Christmas] -->|Get money| B(Go shopping)
                B --> C{Let me think}
                C -->|One| D[Laptop]
                C -->|Two| E[iPhone]
                C -->|Three| F[Car]
        """.trimIndent(),
    )

    val sequence = SampleDiagram(
        name = "Sequence",
        source = """
            %% Order flow
            sequenceDiagram
                actor User
                participant App
                participant API
                User->>App: Open diagram
                App->>API: Load recents
                API-->>App: Last edited source
                App-->>User: Resume editing
                User->>App: Toggle View
                App-->>User: Rendered diagram
        """.trimIndent(),
    )

    val classDiagram = SampleDiagram(
        name = "Class",
        source = """
            %% Domain
            classDiagram
                class Diagram {
                    +String id
                    +String title
                    +String source
                    +Long updatedAt
                }
                class Renderer {
                    +render(source)
                    +exportPng()
                    +exportSvg()
                }
                class Recents {
                    +upsert(diagram)
                    +open(id)
                }
                Diagram --> Renderer : displayed by
                Recents --> Diagram : stores
        """.trimIndent(),
    )

    val all: List<SampleDiagram> = listOf(flowchart, mermaidLiveClassic, sequence, classDiagram)

    val defaultSource: String = flowchart.source
}
