package com.charlseempire

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
/**
 * SE 3242: Android Application Development
 * Week 2 - Exercise 1: Build Your Own Higher-Order Function
 */

/**
 * processList - filters a list based on a given predicate lambda.
 *
 * @param numbers   The list of integers to process.
 * @param predicate A lambda: takes an Int, returns true (keep) or false (discard).
 * @return          A new list with only elements that passed the predicate.
 */
fun processList(
    numbers: List<Int>,
    predicate: (Int) -> Boolean
): List<Int> {
    return numbers.filter { predicate(it) }
}

fun main() {
    val nums = listOf(1, 2, 3, 4, 5, 6)

    // Test 1: Filter even numbers
    // { it % 2 == 0 } means: keep only numbers with no remainder when divided by 2
    val even = processList(nums) { it % 2 == 0 }
    println("Even numbers: $even") // [2, 4, 6]

    // Test 2: Filter odd numbers
    val odd = processList(nums) { it % 2 != 0 }
    println("Odd numbers: $odd") // [1, 3, 5]

    // Test 3: Filter numbers greater than 3
    val greaterThanThree = processList(nums) { it > 3 }
    println("Numbers greater than 3: $greaterThanThree") // [4, 5, 6]
}