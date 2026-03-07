package com.charlseempire

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
/**
 * SE 3242: Android Application Development
 * Week 2 - Exercise 2: Transforming Between Collection Types
 */

fun main() {
    val words = listOf("apple", "cat", "banana", "dog", "elephant")

    // Step 1: associateWith builds a Map from the list
    // Each word becomes the KEY, its length becomes the VALUE
    // Result: {apple=5, cat=3, banana=6, dog=3, elephant=8}
    val wordMap = words.associateWith { it.length }

    // Step 2: filter keeps only entries where the length (value) is greater than 4
    // Step 3: forEach prints each remaining entry
    // it.key = the word, it.value = the length
    wordMap
        .filter { it.value > 4 }
        .forEach { println("${it.key} has length ${it.value}") }
}