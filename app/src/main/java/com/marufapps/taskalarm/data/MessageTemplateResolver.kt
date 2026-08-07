package com.marufapps.taskalarm.data

/**
 * Resolves the actual notification text for a given missed-deadline stage (1-4).
 *
 * Priority order:
 *   1. The task's own custom message for that stage, if set.
 *   2. The user's global default template for that stage (Settings screen), if set.
 *   3. A hardcoded built-in fallback, so notifications always say something sensible.
 *
 * Supports two placeholders inside any template:
 *   {task}   -> the task's title
 *   {hours}  -> how many whole hours overdue the task currently is
 */
object MessageTemplateResolver {

    fun builtInFallback(stage: Int): String = when (stage) {
        1 -> "Still there? {task} needs your attention"
        2 -> "Still not done — {task} is now overdue"
        3 -> "🚨 {task} is {hours}h overdue"
        else -> "Last one — get {task} done!"
    }

    fun resolve(
        task: Task,
        stage: Int,
        globalDefault: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val template = task.customMessageForStage(stage)
            ?: globalDefault?.takeIf { it.isNotBlank() }
            ?: builtInFallback(stage)

        val hoursOverdue = ((nowMillis - task.deadlineEpochMillis) / (60 * 60 * 1000L)).coerceAtLeast(0)

        return template
            .replace("{task}", task.title)
            .replace("{hours}", hoursOverdue.toString())
    }
}
