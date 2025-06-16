package com.example.jenmix.jen8

object HealthCardGenerator {
    fun generateCards(
        gender: String,
        age: Int,
        height: Float,
        weight: Float,
        impedance: Int,
        bmrFromDevice: Int
    ): List<HealthItem> {
        val bmi = HealthAnalyzer.calculateBMI(weight, height)
        val fat = HealthAnalyzer.estimateFatPercentage(gender, age, bmi)
        val fatIndex = HealthAnalyzer.estimateFatIndex(fat)
        val obesityLevel = HealthAnalyzer.estimateObesityLevel(bmi)
        val (weightMin, weightMax) = HealthAnalyzer.estimateNormalWeightRange(height)

        return listOf(
            HealthItem("BMI", "%.1f".format(bmi), getBMIStatus(bmi)),
            HealthItem("體脂指數", "$fatIndex", getFatIndexStatus(fatIndex)),
            HealthItem("肥胖級別", "$obesityLevel", getObesityStatus(obesityLevel)),
            HealthItem("正常體重", "%.1f~%.1f 公斤".format(weightMin, weightMax), getNormalWeightStatus(weight, weightMin, weightMax))
        )
    }

    private fun getBMIStatus(bmi: Float) = when {
        bmi < 18.5f -> "偏瘦"
        bmi < 24f -> "正常"
        bmi < 28f -> "偏胖"
        bmi < 35f -> "肥胖"
        else -> "極胖"
    }

    private fun getFatIndexStatus(index: Int) = when (index) {
        in 0..2 -> "過低"
        3 -> "偏低"
        4 -> "良好"
        5 -> "偏高"
        6 -> "過高"
        else -> "極高"
    }

    private fun getObesityStatus(level: Int) = when (level) {
        0 -> "偏瘦"
        1 -> "不肥胖"
        2 -> "微胖"
        3 -> "偏胖"
        else -> "肥胖"
    }

    private fun getNormalWeightStatus(weight: Float, min: Float, max: Float) = when {
        weight < min -> "偏輕"
        weight <= max -> "正常"
        else -> "偏重"
    }
}
