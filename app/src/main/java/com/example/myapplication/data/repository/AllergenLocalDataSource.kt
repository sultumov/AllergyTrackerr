package com.example.myapplication.data.repository

import com.example.myapplication.data.model.Allergen
import com.example.myapplication.data.model.AllergenCategory

/**
 * Локальный источник данных с предустановленным списком аллергенов
 */
class AllergenLocalDataSource {

    /**
     * Получение списка всех предустановленных аллергенов
     */
    fun getAllAllergens(): List<Allergen> {
        return foodAllergens + pollenAllergens + animalAllergens + 
               insectAllergens + drugAllergens + otherAllergens
    }
    
    /**
     * Получение списка аллергенов определенной категории
     */
    fun getAllergensForCategory(category: AllergenCategory): List<Allergen> {
        return when (category) {
            AllergenCategory.FOOD -> foodAllergens
            AllergenCategory.POLLEN -> pollenAllergens
            AllergenCategory.ANIMAL -> animalAllergens
            AllergenCategory.INSECT -> insectAllergens
            AllergenCategory.DRUG -> drugAllergens
            AllergenCategory.MOLD -> moldAllergens
            AllergenCategory.LATEX -> latexAllergens
            AllergenCategory.DUST -> dustAllergens
            AllergenCategory.CHEMICAL -> chemicalAllergens
            AllergenCategory.OTHER -> otherAllergens
        }
    }
    
    /**
     * Получение аллергена по ID
     */
    fun getAllergenById(id: String): Allergen? {
        return getAllAllergens().find { it.id == id }
    }
    
    /**
     * Поиск аллергенов по имени
     */
    fun searchAllergensByName(query: String): List<Allergen> {
        if (query.isBlank()) return emptyList()
        
        val searchTerm = query.lowercase()
        return getAllAllergens().filter { 
            it.name.lowercase().contains(searchTerm) || 
            it.scientificName?.lowercase()?.contains(searchTerm) == true 
        }
    }
    
    // Список категорий аллергенов
    fun getAllCategories(): List<AllergenCategory> {
        return AllergenCategory.values().toList()
    }
    
    companion object {
        // Пищевые аллергены
        private val foodAllergens = listOf(
            Allergen(
                id = "milk",
                name = "Молоко",
                category = AllergenCategory.FOOD,
                description = "Аллергия на молоко — это неблагоприятная иммунная реакция на белки, содержащиеся в коровьем молоке.",
                symptoms = listOf(
                    "Крапивница или кожная сыпь",
                    "Зуд или покалывание вокруг рта",
                    "Отек губ, языка или горла",
                    "Боль в животе, диарея, тошнота или рвота",
                    "Заложенность носа, чихание, кашель"
                ),
                avoidanceRecommendations = listOf(
                    "Избегайте всех молочных продуктов",
                    "Читайте этикетки на предмет содержания казеина, сыворотки и лактозы",
                    "Ищите альтернативы на основе растительного молока",
                    "Обратите внимание на скрытое содержание молока в продуктах"
                ),
                relatedAllergens = listOf("сыр", "йогурт", "сливочное масло", "мороженое"),
                scientificName = "Аллергия на белки коровьего молока (ABKM)"
            ),
            Allergen(
                id = "egg",
                name = "Яйца",
                category = AllergenCategory.FOOD,
                description = "Аллергия на яйца — это аллергическая реакция на белки, содержащиеся в яйцах птиц, особенно куриных яйцах.",
                symptoms = listOf(
                    "Кожная сыпь или экзема",
                    "Желудочно-кишечные симптомы (тошнота, спазмы, рвота)",
                    "Респираторные проблемы",
                    "Анафилактический шок (в тяжелых случаях)"
                ),
                avoidanceRecommendations = listOf(
                    "Избегайте всех яичных продуктов",
                    "Читайте этикетки на предмет содержания альбумина, глобулина, овоальбумина",
                    "Будьте осторожны с выпечкой и соусами",
                    "Используйте заменители яиц в кулинарии"
                ),
                scientificName = "Овоаллергия"
            ),
            Allergen(
                id = "peanut",
                name = "Арахис",
                category = AllergenCategory.FOOD,
                description = "Аллергия на арахис — одна из самых распространенных и потенциально опасных пищевых аллергий, которая часто вызывает серьезные реакции.",
                symptoms = listOf(
                    "Крапивница, зуд или отек кожи",
                    "Стеснение в горле или затрудненное дыхание",
                    "Тошнота или рвота",
                    "Анафилаксия в тяжелых случаях"
                ),
                avoidanceRecommendations = listOf(
                    "Полностью избегайте арахиса и арахисового масла",
                    "Внимательно проверяйте этикетки продуктов",
                    "Учитывайте риск перекрестного загрязнения",
                    "Носите с собой автоинжектор эпинефрина при тяжелой аллергии"
                ),
                scientificName = "Arachis hypogaea"
            ),
            Allergen(
                id = "tree_nut",
                name = "Древесные орехи",
                category = AllergenCategory.FOOD,
                description = "Аллергия на древесные орехи — это аллергическая реакция на орехи, растущие на деревьях, включая миндаль, грецкие орехи, фундук, кешью и др.",
                symptoms = listOf(
                    "Зуд и отек во рту, горле и глазах",
                    "Крапивница и кожная сыпь",
                    "Затрудненное дыхание",
                    "Анафилактический шок"
                ),
                avoidanceRecommendations = listOf(
                    "Избегайте всех древесных орехов",
                    "Проверяйте этикетки на всех продуктах питания",
                    "Будьте осторожны с ореховыми маслами и экстрактами",
                    "Учитывайте возможное перекрестное загрязнение"
                ),
                relatedAllergens = listOf("миндаль", "фундук", "грецкий орех", "кешью", "фисташки")
            ),
            Allergen(
                id = "fish",
                name = "Рыба",
                category = AllergenCategory.FOOD,
                description = "Аллергия на рыбу — это иммунная реакция на белки, содержащиеся в различных видах рыбы.",
                symptoms = listOf(
                    "Крапивница и отеки",
                    "Тошнота, рвота, диарея",
                    "Заложенность носа, затрудненное дыхание",
                    "Анафилаксия"
                ),
                avoidanceRecommendations = listOf(
                    "Избегайте всех видов рыбы",
                    "Проверяйте этикетки на наличие рыбьего жира и масла",
                    "Будьте осторожны в ресторанах и с соусами",
                    "Избегайте мест приготовления рыбы из-за испарений"
                ),
                relatedAllergens = listOf("лосось", "тунец", "треска", "сельдь", "окунь")
            ),
            Allergen(
                id = "shellfish",
                name = "Моллюски и ракообразные",
                category = AllergenCategory.FOOD,
                description = "Аллергия на морепродукты — это иммунная реакция на белки в ракообразных (креветки, крабы, лобстеры) и моллюсках (мидии, устрицы, кальмары).",
                symptoms = listOf(
                    "Кожные реакции (крапивница, отек)",
                    "Пищеварительные симптомы",
                    "Анафилаксия",
                    "Отек гортани"
                ),
                avoidanceRecommendations = listOf(
                    "Избегайте всех видов моллюсков и ракообразных",
                    "Будьте осторожны с суши и азиатской кухней",
                    "Проверяйте соусы и приправы",
                    "Избегайте ресторанов морепродуктов из-за перекрестного загрязнения"
                ),
                relatedAllergens = listOf("креветки", "крабы", "лобстеры", "устрицы", "мидии", "кальмары")
            ),
            Allergen(
                id = "wheat",
                name = "Пшеница",
                category = AllergenCategory.FOOD,
                description = "Аллергия на пшеницу — это негативная иммунная реакция на белки, содержащиеся в пшенице.",
                symptoms = listOf(
                    "Зуд и отек во рту и горле",
                    "Крапивница и сыпь на коже",
                    "Затрудненное дыхание",
                    "Боль в животе, тошнота, рвота, диарея"
                ),
                avoidanceRecommendations = listOf(
                    "Исключите пшеничную муку и продукты из нее",
                    "Выбирайте безглютеновые альтернативы",
                    "Внимательно читайте этикетки",
                    "Избегайте продуктов с содержанием глютена"
                ),
                relatedAllergens = listOf("глютен", "ячмень", "рожь", "солод")
            ),
            Allergen(
                id = "soy",
                name = "Соя",
                category = AllergenCategory.FOOD,
                description = "Аллергия на сою — это иммунная реакция на белки в соевых бобах и продуктах из них.",
                symptoms = listOf(
                    "Зуд в ротовой полости",
                    "Крапивница, экзема",
                    "Отек губ, языка, горла",
                    "Тошнота, рвота, диарея"
                ),
                avoidanceRecommendations = listOf(
                    "Избегайте соевых продуктов: тофу, соевое молоко, темпе",
                    "Проверяйте соусы и приправы",
                    "Читайте этикетки (лецитин, растительный белок)",
                    "Будьте осторожны с азиатской кухней"
                ),
                scientificName = "Glycine max"
            )
        )
        
        // Аллергены пыльцы растений
        private val pollenAllergens = listOf(
            Allergen(
                id = "birch_pollen",
                name = "Пыльца березы",
                category = AllergenCategory.POLLEN,
                description = "Аллергия на пыльцу березы вызывает сезонный аллергический ринит (сенная лихорадка) и является распространенной весенней аллергией.",
                symptoms = listOf(
                    "Чихание и насморк",
                    "Зуд и покраснение глаз",
                    "Заложенность носа",
                    "Кашель"
                ),
                avoidanceRecommendations = listOf(
                    "Следите за календарем пыления",
                    "Держите окна закрытыми в сезон пыления",
                    "Используйте воздушные фильтры",
                    "Принимайте душ и меняйте одежду после прогулок"
                ),
                scientificName = "Betula"
            ),
            Allergen(
                id = "ragweed_pollen",
                name = "Пыльца амброзии",
                category = AllergenCategory.POLLEN,
                description = "Аллергия на пыльцу амброзии — одна из наиболее распространенных причин сезонной аллергии, особенно в конце лета и осенью.",
                symptoms = listOf(
                    "Сезонный ринит",
                    "Конъюнктивит",
                    "Отек век",
                    "Усталость и головные боли"
                ),
                avoidanceRecommendations = listOf(
                    "Ограничьте время пребывания на улице в сезон пыления",
                    "Используйте кондиционер в автомобиле и дома",
                    "Носите защитные очки и маску при работе на улице",
                    "Следите за прогнозом распространения пыльцы"
                ),
                scientificName = "Ambrosia"
            ),
            Allergen(
                id = "grass_pollen",
                name = "Пыльца злаковых трав",
                category = AllergenCategory.POLLEN,
                description = "Аллергия на пыльцу злаков — это реакция на пыльцу различных видов трав, которая распространяется в основном в весенне-летний период.",
                symptoms = listOf(
                    "Аллергический ринит и конъюнктивит",
                    "Затрудненное дыхание",
                    "Кожный зуд",
                    "Астматические симптомы"
                ),
                avoidanceRecommendations = listOf(
                    "Избегайте свежескошенных газонов",
                    "Закрывайте окна дома в период цветения",
                    "Носите солнцезащитные очки на улице",
                    "Пользуйтесь календарем пыления растений"
                ),
                scientificName = "Poaceae"
            )
        )
        
        // Аллергены животных
        private val animalAllergens = listOf(
            Allergen(
                id = "cat_dander",
                name = "Кошачья шерсть",
                category = AllergenCategory.ANIMAL,
                description = "Аллергия на кошек вызывается белком Fel d 1, который содержится в слюне, сальных железах кошек и перхоти, а не только в шерсти.",
                symptoms = listOf(
                    "Чихание и насморк",
                    "Раздражение и покраснение глаз",
                    "Кашель и хрипы",
                    "Кожная сыпь"
                ),
                avoidanceRecommendations = listOf(
                    "Ограничьте контакт с кошками",
                    "Используйте высокоэффективные воздушные фильтры",
                    "Регулярно проводите влажную уборку",
                    "Создайте зоны, свободные от животных, особенно спальню"
                ),
                scientificName = "Felis catus (аллерген Fel d 1)"
            ),
            Allergen(
                id = "dog_dander",
                name = "Собачья шерсть",
                category = AllergenCategory.ANIMAL,
                description = "Аллергия на собак обычно вызывается белками в слюне, моче и перхоти собак, а не только шерстью.",
                symptoms = listOf(
                    "Зуд в носу и глазах",
                    "Заложенность носа",
                    "Астматические симптомы",
                    "Кожные реакции при контакте"
                ),
                avoidanceRecommendations = listOf(
                    "Ограничьте контакт с собаками",
                    "Регулярно купайте собаку, если она живет с вами",
                    "Используйте специальные шампуни, снижающие количество аллергенов",
                    "Регулярно пылесосьте с HEPA-фильтром"
                ),
                scientificName = "Canis familiaris (аллерген Can f 1)"
            )
        )
        
        // Аллергены насекомых
        private val insectAllergens = listOf(
            Allergen(
                id = "bee_venom",
                name = "Пчелиный яд",
                category = AllergenCategory.INSECT,
                description = "Аллергия на пчелиный яд — потенциально опасная реакция на токсины, вводимые при укусе пчелы.",
                symptoms = listOf(
                    "Сильная боль и отек в месте укуса",
                    "Крапивница и зуд по всему телу",
                    "Отек лица, губ или горла",
                    "Анафилактический шок"
                ),
                avoidanceRecommendations = listOf(
                    "Избегайте мест скопления пчел",
                    "Не носите яркую одежду и сильные ароматы на природе",
                    "Будьте осторожны с открытыми сладкими напитками на улице",
                    "При тяжелой аллергии носите с собой автоинжектор с эпинефрином"
                ),
                scientificName = "Apis mellifera venom"
            ),
            Allergen(
                id = "wasp_venom",
                name = "Осиный яд",
                category = AllergenCategory.INSECT,
                description = "Аллергия на осиный яд может вызвать серьезную реакцию, включая анафилаксию.",
                symptoms = listOf(
                    "Сильный отек в месте укуса",
                    "Генерализованная крапивница",
                    "Затрудненное дыхание",
                    "Снижение артериального давления"
                ),
                avoidanceRecommendations = listOf(
                    "Будьте осторожны при еде на открытом воздухе",
                    "Избегайте хождения босиком на траве",
                    "Не делайте резких движений при виде ос",
                    "Закрывайте пищевые отходы"
                ),
                scientificName = "Vespula venom"
            ),
            Allergen(
                id = "dust_mites",
                name = "Пылевые клещи",
                category = AllergenCategory.DUST,
                description = "Аллергия на пылевых клещей вызывается микроскопическими организмами, живущими в домашней пыли и питающимися чешуйками кожи.",
                symptoms = listOf(
                    "Заложенность носа и чихание",
                    "Зуд в глазах и насморк",
                    "Кашель, особенно ночью",
                    "Затрудненное дыхание и свистящее дыхание"
                ),
                avoidanceRecommendations = listOf(
                    "Используйте противоаллергенные чехлы для матрасов и подушек",
                    "Стирайте постельное белье при высокой температуре",
                    "Поддерживайте влажность в помещении ниже 50%",
                    "Регулярно пылесосьте с HEPA-фильтром"
                ),
                scientificName = "Dermatophagoides"
            )
        )
        
        // Лекарственные аллергены
        private val drugAllergens = listOf(
            Allergen(
                id = "penicillin",
                name = "Пенициллин",
                category = AllergenCategory.DRUG,
                description = "Аллергия на пенициллин — наиболее распространенная лекарственная аллергия, которая может вызывать серьезные, иногда опасные для жизни реакции.",
                symptoms = listOf(
                    "Крапивница и зуд",
                    "Отек губ, языка или лица",
                    "Затрудненное дыхание",
                    "Анафилаксия"
                ),
                avoidanceRecommendations = listOf(
                    "Информируйте всех врачей о вашей аллергии",
                    "Носите медицинский браслет с информацией об аллергии",
                    "Избегайте всех препаратов группы пенициллина",
                    "Узнайте о возможных перекрестных реакциях с другими антибиотиками"
                ),
                relatedAllergens = listOf("амоксициллин", "ампициллин", "диклоксациллин")
            ),
            Allergen(
                id = "nsaids",
                name = "НПВП (аспирин, ибупрофен)",
                category = AllergenCategory.DRUG,
                description = "Аллергия на нестероидные противовоспалительные препараты (НПВП) может вызывать различные реакции от легких до тяжелых.",
                symptoms = listOf(
                    "Крапивница и отеки",
                    "Обострение астмы",
                    "Ринит",
                    "Анафилаксия (редко)"
                ),
                avoidanceRecommendations = listOf(
                    "Избегайте всех НПВП при подтвержденной аллергии",
                    "Проконсультируйтесь с врачом о безопасных альтернативах",
                    "Тщательно читайте состав препаратов",
                    "Обсудите возможность десенсибилизации в некоторых случаях"
                ),
                relatedAllergens = listOf("аспирин", "ибупрофен", "напроксен", "диклофенак")
            )
        )
        
        // Аллергены плесени
        private val moldAllergens = listOf(
            Allergen(
                id = "alternaria",
                name = "Alternaria",
                category = AllergenCategory.MOLD,
                description = "Alternaria — распространенный вид плесени, вызывающий аллергию, особенно в теплое влажное время года.",
                symptoms = listOf(
                    "Чихание и насморк",
                    "Зуд в глазах",
                    "Кашель",
                    "Обострение астмы"
                ),
                avoidanceRecommendations = listOf(
                    "Контролируйте влажность в помещениях",
                    "Используйте осушители воздуха",
                    "Регулярно проверяйте наличие плесени в ванной и на кухне",
                    "Проводите регулярную уборку с противогрибковыми средствами"
                ),
                scientificName = "Alternaria alternata"
            )
        )
        
        // Аллергены латекса
        private val latexAllergens = listOf(
            Allergen(
                id = "latex",
                name = "Латекс",
                category = AllergenCategory.LATEX,
                description = "Аллергия на латекс — это реакция на белки, содержащиеся в натуральном латексе, который используется для производства резиновых изделий.",
                symptoms = listOf(
                    "Зуд и покраснение кожи",
                    "Крапивница",
                    "Ринит и конъюнктивит",
                    "Анафилаксия"
                ),
                avoidanceRecommendations = listOf(
                    "Используйте безлатексные перчатки и изделия",
                    "Информируйте медицинский персонал о своей аллергии",
                    "Избегайте контакта с латексными изделиями",
                    "Будьте осторожны с перекрестной аллергией на некоторые фрукты"
                ),
                relatedAllergens = listOf("бананы", "авокадо", "киви", "каштаны")
            )
        )
        
        // Аллергены пыли
        private val dustAllergens = listOf(
            Allergen(
                id = "house_dust",
                name = "Домашняя пыль",
                category = AllergenCategory.DUST,
                description = "Домашняя пыль содержит множество аллергенов, включая клещей, частицы кожи, плесень и другие органические материалы.",
                symptoms = listOf(
                    "Чихание и насморк",
                    "Кашель",
                    "Заложенность носа",
                    "Зуд в глазах и носу"
                ),
                avoidanceRecommendations = listOf(
                    "Регулярно пылесосьте с HEPA-фильтром",
                    "Используйте влажную уборку",
                    "Уменьшите количество текстиля и ковров",
                    "Используйте воздухоочистители"
                )
            )
        )
        
        // Аллергены химических веществ
        private val chemicalAllergens = listOf(
            Allergen(
                id = "formaldehyde",
                name = "Формальдегид",
                category = AllergenCategory.CHEMICAL,
                description = "Формальдегид — это химическое вещество, используемое в производстве многих материалов и бытовых продуктов, которое может вызывать аллергические реакции.",
                symptoms = listOf(
                    "Раздражение глаз, носа и горла",
                    "Кашель и хрипы",
                    "Головная боль",
                    "Кожная сыпь"
                ),
                avoidanceRecommendations = listOf(
                    "Проветривайте новую мебель и текстиль перед использованием",
                    "Выбирайте продукты с низким содержанием формальдегида",
                    "Используйте воздухоочистители",
                    "Проветривайте помещения"
                ),
                scientificName = "CH₂O (метаналь)"
            )
        )
        
        // Другие аллергены
        private val otherAllergens = listOf(
            Allergen(
                id = "sunlight",
                name = "Солнечный свет",
                category = AllergenCategory.OTHER,
                description = "Фотоаллергия — это аллергическая реакция, вызванная воздействием солнечного света, особенно ультрафиолетового излучения.",
                symptoms = listOf(
                    "Зуд и покраснение кожи",
                    "Сыпь и волдыри",
                    "Отек кожи",
                    "Шелушение"
                ),
                avoidanceRecommendations = listOf(
                    "Используйте солнцезащитные средства с высоким SPF",
                    "Носите защитную одежду",
                    "Избегайте пребывания на солнце в пиковые часы",
                    "Используйте зонты и широкополые шляпы"
                ),
                scientificName = "Фотодерматит"
            )
        )
    }
} 