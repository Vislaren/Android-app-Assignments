package com.charlseempire

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
/**
 * SE 3242: Android Application Development
 * Week 2 - Exercise 3: Complex Data Processing
 */

// data class automatically generates equals(), toString(), copy() etc.
data class Person(val name: String, val age: Int)

fun main() {
    val people = listOf(
        Person("Alice", 25),
        Person("Bob", 30),
        Person("Charlie", 35),
        Person("Anna", 22),
        Person("Ben", 28)
    )

    val average = people
        // Step 1: Keep only people whose name starts with A or B
        // startsWith() checks the first character of the string
        .filter { it.name.startsWith("A") || it.name.startsWith("B") }
        // Remaining: [Alice/25, Bob/30, Anna/22, Ben/28]

        // Step 2: Extract just the ages — we no longer need full Person objects
        .map { it.age }
        // Now we have: [25, 30, 22, 28]

        // Step 3: Kotlin's built-in average() on a number list
        .average()
    // (25 + 30 + 22 + 28) / 4 = 26.25

    // Step 4: Print rounded to 1 decimal place
    // %.1f = format as decimal with 1 digit after the point
    println("Average age: ${"%.1f".format(average)}")
    // Output: Average age: 26.3
}