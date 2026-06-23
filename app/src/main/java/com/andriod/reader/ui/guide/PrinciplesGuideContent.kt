package com.andriod.reader.ui.guide

data class GuideSection(
    val title: String,
    val paragraphs: List<String>,
    val codeExample: String? = null,
)

object PrinciplesGuideContent {
    val sections: List<GuideSection> = listOf(
        GuideSection(
            title = "功能概览",
            paragraphs = listOf(
                "阅读页将笔记按块展示；准则类块可点击。",
                "点击后记录今日遵守或今日违背，可选备注。",
                "块旁圆点：绿色 = 今日遵守，红色 = 今日违背，浅灰 = 今日尚未记录。",
                "践行记录保存在本机 .meta 目录，不会随 GitHub 同步；笔记正文仍为 .md 文件。",
            ),
        ),
        GuideSection(
            title = "哪些块可点击",
            paragraphs = listOf(
                "- [ ] / - [x] 待办行：仅展示，不记录践行（工具栏「待办」）",
                "> [!rule] 准则 Callout（工具栏「准则」）",
                "> [!habit] 习惯 Callout（工具栏「习惯」）",
                "普通段落、标题、普通列表项不可点击。",
            ),
        ),
        GuideSection(
            title = "编辑器工具栏",
            paragraphs = listOf(
                "编辑笔记、键盘弹出时，底部工具栏可一键插入格式，无需手打符号。",
                "待办：添加或去掉 - [ ]",
                "准则：添加或去掉 > [!rule]",
                "习惯：添加或去掉 > [!habit]",
                "光标放在当前行后点击即可；再次点击可去掉前缀。",
            ),
        ),
        GuideSection(
            title = "示例笔记",
            paragraphs = emptyList(),
            codeExample = """
                # 健康作息

                > [!habit] 11 点睡觉

                - [ ] 睡前听听书，别刷手机

                # 沟通

                > [!rule] 别轻易跟旁边人诉苦
            """.trimIndent(),
        ),
        GuideSection(
            title = "- [ ] 与 - [x]",
            paragraphs = listOf(
                "二者是 Markdown 待办，写在笔记里，会 GitHub 同步。",
                "[ ] 显示为 ☐，表示清单未完成；[x] 显示为 ☑（删除线），表示清单已完成或已内化。",
                "与「今日遵守/违背」不是一回事：绿/红点是点块记录的践行，存在本机 .meta，不是改 [ ]/[x]。",
                "[ ] 与 [x] 都可以点击记录今日践行。",
            ),
        ),
        GuideSection(
            title = "[!rule] 与 [!habit]",
            paragraphs = listOf(
                "P1 中记录方式相同（遵守 / 违背）。",
                "[!habit] 建议用于每天可检的习惯（作息）。",
                "[!rule] 建议用于遇到场景才用的原则（沟通、决策）。",
                "P2 将按类型细化交互（如「今天遇到了吗」、养成色条等）。",
            ),
        ),
        GuideSection(
            title = "可选：行末 ^id",
            paragraphs = listOf(
                "例如 > [!habit] 11 点睡觉 ^sleep11",
                "改措辞后践行记录仍绑定同一条；不写时 App 自动生成 ID。",
            ),
        ),
        GuideSection(
            title = "数据与同步",
            paragraphs = listOf(
                "准则正文：notes/*.md，会同步 GitHub。",
                "今日践行：.meta/practice-logs.json，仅本机。",
            ),
        ),
    )

    const val SCREEN_TITLE = "行为准则使用说明"
}
