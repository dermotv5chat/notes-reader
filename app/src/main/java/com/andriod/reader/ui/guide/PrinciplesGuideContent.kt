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

                > [!rule] 别轻易跟旁边人诉苦，未必被理解，还可能引发矛盾

                > [!rule] 行动才是解决焦虑的方法
            """.trimIndent(),
        ),
        GuideSection(
            title = "- [ ] 与 - [x]",
            paragraphs = listOf(
                "二者是 Markdown 待办，写在笔记里，会 GitHub 同步。",
                "[ ] 显示为 ☐，表示清单未完成；[x] 显示为 ☑（删除线），表示清单已完成或已内化。",
                "与「今日遵守/违背」不是一回事：绿/红点是点准则 Callout 记录的践行，存在本机 .meta。",
                "待办 [ ] / [x] 不能点击记录践行。",
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
            title = "一行一条准则",
            paragraphs = listOf(
                "App 按 Markdown 文件里的换行（按回车）拆块，不是按屏幕显示行数。",
                "准则写在一行里时，即使阅读页自动折成多行显示，仍算 1 个块、1 个践行圆点。",
                "若在准则中间按回车，下一行没有 > [!rule] 或 > [!habit] 前缀，会变成普通段落，不能点践行。",
                "建议：一条准则占一行；文字可以很长，界面会自动折行，无需手动换行。",
            ),
            codeExample = """
                # 正确：一行，界面自动折行仍是一个块
                > [!rule] 别轻易跟旁边人诉苦，未必被理解，还可能引发矛盾

                # 不推荐：中间回车会变成两个块（第二行不可点践行）
                > [!rule] 别轻易跟旁边人诉苦
                未必被理解，还可能引发矛盾
            """.trimIndent(),
        ),
        GuideSection(
            title = "隐式块 ID",
            paragraphs = listOf(
                "每个准则块在 App 内自动分配 ID，存在 .meta/block-registry.json。",
                "Markdown 正文中不会出现 ID；只改准则文字时，践行圆点不会丢。",
            ),
        ),
        GuideSection(
            title = "历史记录",
            paragraphs = listOf(
                "点击准则块后，弹窗内可展开「历史记录」查看（时间、遵守/违背、备注），默认收起。",
                "每次点「遵守」或「违背」都会追加一条，同一天可有多条；块旁圆点取今日最新一条。",
                "展开历史后，列表下方可「清除今日记录」（需二次确认），会删掉该准则今天的全部条目。",
            ),
        ),
        GuideSection(
            title = "数据与同步",
            paragraphs = listOf(
                "准则正文：notes/*.md，会同步 GitHub。",
                "块 ID 与今日践行：.meta/ 下仅本机，不同步 GitHub。",
            ),
        ),
    )

    const val SCREEN_TITLE = "行为准则使用说明"
}
