package com.github.kubapieces.intellijdraganddroplist.sdkutil

import org.piecesapp.client.models.ClassificationSpecificEnum
import kotlin.reflect.KClass

fun <T : Enum<*>> KClass<T>.safeValue(ext: String?): ClassificationSpecificEnum? = runCatching { ClassificationSpecificEnum.valueOf(ext.orEmpty()) }.getOrNull()