/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation.test

import androidx.navigation.NavArgument
import androidx.navigation.NavType.Companion.BoolType
import androidx.navigation.NavType.Companion.FloatType
import androidx.navigation.NavType.Companion.IntListType
import androidx.navigation.NavType.Companion.IntType
import androidx.navigation.NavType.Companion.LongType
import androidx.navigation.NavType.Companion.StringArrayType
import androidx.navigation.NavType.Companion.StringListType
import androidx.navigation.NavType.Companion.StringType

// region IntType
fun intArgument() = NavArgument.Builder().setType(IntType).build()

fun booleanArgument() = NavArgument.Builder().setType(BoolType).build()

fun intArgument(defaultValue: Int) =
    NavArgument.Builder().setType(IntType).setDefaultValue(defaultValue).build()

fun intArgumentUnknownDefault() =
    NavArgument.Builder().setType(IntType).setUnknownDefaultValuePresent(true).build()

// endregion

// region LongType
fun longArgument() = NavArgument.Builder().setType(LongType).build()

fun longArgument(defaultValue: Long) =
    NavArgument.Builder().setType(LongType).setDefaultValue(defaultValue).build()

// endregion

// region FloatType
fun floatArgument() = NavArgument.Builder().setType(FloatType).build()

fun floatArgument(defaultValue: Float) =
    NavArgument.Builder().setType(FloatType).setDefaultValue(defaultValue).build()

// endregion

// region StringType
fun stringArgument() = NavArgument.Builder().setType(StringType).setIsNullable(false).build()

fun stringArgumentUnknownDefault() =
    NavArgument.Builder()
        .setType(StringType)
        .setUnknownDefaultValuePresent(true)
        .setIsNullable(false)
        .build()

fun stringArgument(defaultValue: String) =
    NavArgument.Builder().setType(StringType).setDefaultValue(defaultValue).build()

fun nullableStringArgument() = NavArgument.Builder().setType(StringType).setIsNullable(true).build()

fun nullableStringArgument(defaultValue: String?) =
    NavArgument.Builder()
        .setType(StringType)
        .setIsNullable(true)
        .setDefaultValue(defaultValue)
        .build()

fun nullableStringArgumentUnknownDefault() =
    NavArgument.Builder()
        .setType(StringType)
        .setIsNullable(true)
        .setUnknownDefaultValuePresent(true)
        .build()

// endregion

// region StringArrayType
fun stringArrayArgument(defaultValue: Array<String>?) =
    NavArgument.Builder()
        .setType(StringArrayType)
        .setIsNullable(true)
        .setDefaultValue(defaultValue)
        .build()

// endregion

// region StringListType
fun stringListArgument() = NavArgument.Builder().setType(StringListType).setIsNullable(true).build()

// endregion

// region StringListType
fun stringListArgument(defaultValue: List<String>) =
    NavArgument.Builder()
        .setType(StringListType)
        .setIsNullable(true)
        .setDefaultValue(defaultValue)
        .build()

// endregion

// region IntListType
fun intListArgument() = NavArgument.Builder().setType(IntListType).setIsNullable(true).build()

// endregion

// region IntListType
fun intListArgument(defaultValue: List<Int>) =
    NavArgument.Builder()
        .setType(IntListType)
        .setIsNullable(true)
        .setDefaultValue(defaultValue)
        .build()
// endregion
