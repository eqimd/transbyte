package parsed_types.data

import constants.Constants

enum class SaveFormat(val format: String) {
    AAG(Constants.FORMAT_AAG),
    CNF(Constants.FORMAT_CNF)
}
