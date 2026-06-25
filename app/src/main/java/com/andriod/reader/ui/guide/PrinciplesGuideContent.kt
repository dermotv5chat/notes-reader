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
                "点击准则块后，轻点按钮快记；长按可加备注；轻点「敲一下」有木鱼声与短震（可在设置 → 声音与震动关闭），不影响绿/红圆点。",
                "REPEATLY 型（默认 habit）：本周期未记显示灰点，记过显示绿/红；可写 |week、|month。",
                "WHEN 型（默认 rule）：多数日子无圆点，记过才亮并显示「上次日期」。",
                "块左侧竖条为近 30 天养成色（绿好、红差、灰中性）。",
                "践行记录保存在本机 .meta 目录，不会随 GitHub 同步。",
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
                "[!habit] 默认 REPEATLY 日周期：每天可检（作息、戒断）。",
                "[!rule] 默认 WHEN：遇到场景才记；多数日子不点块即可。",
                "周期后缀：`> [!habit|week]`、`> [!habit|month]`；`|always` 等同 WHEN。",
            ),
        ),
        GuideSection(
            title = "践行弹窗",
            paragraphs = listOf(
                "统一按钮：遵守 / 违背 / 敲一下。",
                "WHEN：没遇到时不点开；REPEATLY：本周期灰点提醒未记。",
                "历史可展开，支持列表与月历两种视图。",
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
                "点击准则块后，弹窗内可展开「历史记录」查看（时间、遵守/违背/敲一下、备注），默认收起。",
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
