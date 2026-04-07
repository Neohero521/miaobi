package com.miaobi.app.data.local.database

import com.miaobi.app.data.local.entity.StoryTemplateEntity

object StoryTemplateData {
    fun getTemplates(): List<StoryTemplateEntity> = listOf(
        // 都市题材 (urban)
        StoryTemplateEntity(
            title = "都市重生",
            genre = "urban",
            summary = "现代都市，主角意外重生回到过去，带着前世记忆弥补遗憾，在都市中步步为营，最终走上人生巅峰。适合喜欢逆袭、打脸、创业题材的作者。",
            charactersJson = """[
                {"name": "主角", "description": "重生者，前世普通上班族，带着记忆回到大学时代，性格坚韧果决"},
                {"name": "女主", "description": "校花/职场精英，与主角命运交织，外冷内热"},
                {"name": "反派", "description": "前世害主之人，贪婪阴险"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "时间线", "content": "现代都市，主角重生回到大学时期(2010年代)", "category": "time"},
                {"name": "城市背景", "content": "一线城市，经济发展迅速，充满机遇", "category": "location"},
                {"name": "社会环境", "content": "互联网崛起，房地产市场繁荣，创业浪潮兴起", "category": "general"}
            ]""".trimIndent(),
            promptTemplate = "请基于主角重生逆袭的故事背景，续写当前章节。保持紧张刺激的节奏，情节要曲折动人。",
            isBuiltIn = true
        ),
        StoryTemplateEntity(
            title = "职场风云",
            genre = "urban",
            summary = "职场小白一路升级打怪，从基层员工到公司高管，经历办公室政治、商业竞争，最终实现职业理想。适合职场商战题材。",
            charactersJson = """[
                {"name": "主角", "description": "职场新人/转行者，聪明上进，从基层做起"},
                {"name": "导师", "description": "公司元老，看中主角潜力，暗中提携"},
                {"name": "对手", "description": "同期入职的竞争者，能力出众但心术不正"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "公司背景", "content": "大型上市公司，行业领先地位", "category": "location"},
                {"name": "职场规则", "content": "业绩为王，关系复杂，机会与陷阱并存", "category": "general"},
                {"name": "行业背景", "content": "某朝阳行业，市场竞争激烈", "category": "general"}
            ]""".trimIndent(),
            promptTemplate = "请基于职场竞争的故事背景，续写当前章节。展现职场智慧和人性的复杂。",
            isBuiltIn = true
        ),

        // 玄幻题材 (fantasy)
        StoryTemplateEntity(
            title = "东方修仙",
            genre = "fantasy",
            summary = "传统东方玄幻世界，少年意外获得上古传承，修炼飞升，历经磨难终成大道。适合修仙、玄幻爽文题材。",
            charactersJson = """[
                {"name": "主角", "description": "根骨奇佳的少年/少女，意外获得传承，性格坚毅果敢"},
                {"name": "师尊", "description": "宗门长老，实力深不可测，护短"},
                {"name": "天才师兄", "description": "宗门天才，前期看不起主角后期打脸"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "修仙境界", "content": "炼气、筑基、金丹、元婴、化神、渡劫、大乘、飞升", "category": "magic"},
                {"name": "宗门势力", "content": "各大修仙宗门林立，竞争激烈", "category": "general"},
                {"name": "地理设定", "content": "凡间、灵界、仙界等多重空间", "category": "location"}
            ]""".trimIndent(),
            promptTemplate = "请基于修仙世界的故事背景，续写当前章节。描写修炼过程要精彩，打斗场面要热血。",
            isBuiltIn = true
        ),
        StoryTemplateEntity(
            title = "异界穿越",
            genre = "fantasy",
            summary = "现代人意外穿越到异世界，获得特殊能力，在魔法与剑的世界中冒险成长。适合穿越、异世界冒险题材。",
            charactersJson = """[
                {"name": "主角", "description": "穿越者，拥有现代知识和高智商，冷静理智"},
                {"name": "精灵伙伴", "description": "异世界原住民，与主角结伴冒险"},
                {"name": "魔王", "description": "最终Boss，前期作为传说存在"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "魔法体系", "content": "元素魔法、剑术、召唤术等多系并存", "category": "magic"},
                {"name": "种族设定", "content": "人族、精灵族、兽族、龙族等多种族共存", "category": "general"},
                {"name": "冒险地图", "content": "王国、森林、沙漠、雪山等多样化场景", "category": "location"}
            ]""".trimIndent(),
            promptTemplate = "请基于异世界冒险的故事背景，续写当前章节。冒险情节要生动有趣，战斗要精彩。",
            isBuiltIn = true
        ),

        // 悬疑题材 (mystery)
        StoryTemplateEntity(
            title = "刑侦探案",
            genre = "mystery",
            summary = "刑侦警探凭借敏锐观察力和推理能力，破解一个个离奇案件，揭露真相。适合破案、推理、悬疑题材。",
            charactersJson = """[
                {"name": "主角", "description": "资深刑警/侦探，观察力惊人，性格沉稳"},
                {"name": "搭档", "description": "年轻警员，热血冲动，与主角互补"},
                {"name": "嫌疑人", "description": "每个案件都有复杂的嫌疑人，需要抽丝剥茧"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "案件背景", "content": "现代都市，案件涉及各行各业", "category": "general"},
                {"name": "警局设定", "content": "重案组专门负责重大刑事案件", "category": "location"},
                {"name": "破案手段", "content": "传统刑侦与现代科技结合", "category": "general"}
            ]""".trimIndent(),
            promptTemplate = "请基于刑侦破案的故事背景，续写当前章节。案件要扑朔迷离，推理要严密。",
            isBuiltIn = true
        ),
        StoryTemplateEntity(
            title = "心理罪案",
            genre = "mystery",
            summary = "心理学专家协助警方破案，深入罪犯内心世界，破解高智商犯罪。适合心理分析、犯罪心理学题材。",
            charactersJson = """[
                {"name": "主角", "description": "心理学专家/侧写师，能洞察人心，性格内敛"},
                {"name": "刑警队长", "description": "经验丰富的警探，最初不信心理学后来折服"},
                {"name": "神秘罪犯", "description": "高智商犯罪者，动机复杂"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "心理画像", "content": "通过行为分析推测罪犯心理特征和作案动机", "category": "general"},
                {"name": "犯罪场景", "content": "各种精心设计的犯罪现场", "category": "location"},
                {"name": "社会背景", "content": "都市社会，各阶层矛盾冲突", "category": "general"}
            ]""".trimIndent(),
            promptTemplate = "请基于心理犯罪的故事背景，续写当前章节。要展现心理博弈的紧张感。",
            isBuiltIn = true
        ),

        // 科幻题材 (sci-fi)
        StoryTemplateEntity(
            title = "星际探索",
            genre = "sci-fi",
            summary = "人类进入星际大航海时代，主角成为星际飞船船员，探索未知星球，对抗外星威胁。适合星际冒险、科技幻想题材。",
            charactersJson = """[
                {"name": "主角", "description": "星际飞船船员/舰长，勇敢智慧，适应力强"},
                {"name": "AI副官", "description": "飞船上的人工智能，忠诚可靠偶有幽默"},
                {"name": "外星种族", "description": "探索中遇到的各种外星生命"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "星际政治", "content": "地球联邦与各殖民星系的复杂关系", "category": "general"},
                {"name": "飞船设定", "content": "曲速/跃迁引擎，可进行星际旅行", "category": "tech"},
                {"name": "星球生态", "content": "各种奇特的星球环境和生态系统", "category": "location"}
            ]""".trimIndent(),
            promptTemplate = "请基于星际探索的故事背景，续写当前章节。科幻设定要新颖，冒险情节要精彩。",
            isBuiltIn = true
        ),
        StoryTemplateEntity(
            title = "赛博朋克",
            genre = "sci-fi",
            summary = "近未来科技高度发达但社会贫富分化严重，主角在高科技与低生活的世界中挣扎求生，揭露巨头企业阴谋。",
            charactersJson = """[
                {"name": "主角", "description": "黑客/义体改造者，游走于网络和现实之间"},
                {"name": "反抗者", "description": "地下反抗组织成员，为自由而战"},
                {"name": "企业高管", "description": "巨型企业的冷酷管理者"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "科技设定", "content": "神经链接、义体改造、脑机接口等技术普及", "category": "tech"},
                {"name": "城市设定", "content": "霓虹灯下的巨型都市，贫民窟与摩天大楼并存", "category": "location"},
                {"name": "社会阶层", "content": "权贵阶层与底层民众的对立", "category": "general"}
            ]""".trimIndent(),
            promptTemplate = "请基于赛博朋克的故事背景，续写当前章节。赛博朋克氛围要浓厚，反抗主题要鲜明。",
            isBuiltIn = true
        ),

        // 言情题材 (romance)
        StoryTemplateEntity(
            title = "现代甜宠",
            genre = "romance",
            summary = "现代都市中的甜蜜爱情故事，从相遇到相知到相守，经历误会和挫折最终修成正果。适合甜蜜恋爱、总裁文题材。",
            charactersJson = """[
                {"name": "女主", "description": "独立自强的现代女性，聪明善良"},
                {"name": "男主", "description": "霸道总裁/暖男，对女主情有独钟"},
                {"name": "助攻", "description": "闺蜜/兄弟，感情神助攻"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "都市背景", "content": "现代一线城市，高楼大厦与烟火气并存", "category": "location"},
                {"name": "社交场景", "content": "公司、咖啡厅、商场等都市生活场景", "category": "general"},
                {"name": "情感纠葛", "content": "误会、前任、家庭阻力等戏剧冲突", "category": "general"}
            ]""".trimIndent(),
            promptTemplate = "请基于甜蜜恋爱的故事背景，续写当前章节。感情互动要甜，情节要温馨。",
            isBuiltIn = true
        ),
        StoryTemplateEntity(
            title = "古代宫廷",
            genre = "romance",
            summary = "古代宫廷背景的爱情故事，后宫争斗与皇室阴谋交织，真情难得。适合宫斗、古风言情题材。",
            charactersJson = """[
                {"name": "女主", "description": "入宫女子/嫔妃，聪慧机敏，在后宫中步步为营"},
                {"name": "男主", "description": "皇帝/王爷，深情霸道"},
                {"name": "情敌", "description": "后宫嫔妃或贵族千金，与女主为竞争关系"}
            ]""".trimIndent(),
            worldSettingsJson = """[
                {"name": "朝代背景", "content": "架空虚构古代王朝，礼仪文化参考各朝代", "category": "general"},
                {"name": "宫廷设定", "content": "后宫殿宇，森严等级，各司其职", "category": "location"},
                {"name": "权力斗争", "content": "后宫的权谋争斗与皇位争夺", "category": "general"}
            ]""".trimIndent(),
            promptTemplate = "请基于古代宫廷的故事背景，续写当前章节。宫斗情节要精彩，感情线要细腻。",
            isBuiltIn = true
        )
    )
}
