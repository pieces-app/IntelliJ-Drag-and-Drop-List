package org.piecesapp.intellij.draganddroplist.sdkutil

import org.piecesapp.client.models.ClassificationSpecificEnum
import kotlin.reflect.KClass

/**
 * This function attempts to convert a string to a ClassificationSpecificEnum value safely.
 * If the conversion fails (e.g., if the string does not match any of the enum's values), it returns null.
 *
 * @param ext The string to convert to a ClassificationSpecificEnum value.
 * @return The corresponding ClassificationSpecificEnum value if the conversion is successful, null otherwise.
 */
fun <T : Enum<*>> KClass<T>.safeValue(ext: String?): ClassificationSpecificEnum? = runCatching { ClassificationSpecificEnum.valueOf(ext.orEmpty()) }.getOrNull()