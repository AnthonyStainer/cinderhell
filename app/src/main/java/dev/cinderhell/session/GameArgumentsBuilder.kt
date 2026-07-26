package dev.cinderhell.session

internal object GameArgumentsBuilder {
    fun build(descriptor: GameSessionDescriptor): Array<String> = buildList {
        val game = descriptor.orderedContent.single { it.role == ContentRole.GAME }
        add("-iwad")
        add(game.path)
        add("-config")
        add(descriptor.configPath)
        add("-save")
        add(descriptor.saveDirectory)
        add("-shotdir")
        add(descriptor.screenshotDirectory)

        descriptor.orderedContent
            .filter { it.role != ContentRole.GAME }
            .forEach { content ->
                add(if (content.role == ContentRole.PATCH) "-deh" else "-file")
                add(content.path)
            }

        descriptor.options.skill?.let {
            add("-skill")
            add(it.toString())
        }
        descriptor.options.warp?.let {
            add("-warp")
            if (it.startsWith("MAP")) {
                add(it.removePrefix("MAP").toInt().toString())
            } else {
                add(it.substringAfter('E').substringBefore('M').toInt().toString())
                add(it.substringAfter('M').toInt().toString())
            }
        }
        descriptor.options.compatibility?.let {
            add("-complevel")
            add(it)
        }
        descriptor.options.loadGameSlot?.let {
            add("-loadgame")
            add(it.toString())
        }
    }.toTypedArray()
}
