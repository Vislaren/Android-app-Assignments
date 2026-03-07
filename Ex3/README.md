# Exercise 3 – Complex Data Processing

**Course:** SE 3242 – Android Application Development  
**Week:** 2 | **Topic:** Data Classes, Collections & Lambdas  
**Student:** Fanyi Charllson & Adrien Tello

---

## Objective

Find the average age of people whose names start with 'A' or 'B' from a
list of Person objects. Print the result rounded to one decimal place.

---

## Concepts Demonstrated

**Data Class**  
`data class Person(val name: String, val age: Int)` auto-generates
`toString()`, `equals()`, and `copy()` — no boilerplate needed.

**Chained Collection Operations**  
Three operations chained together: `filter` → `map` → `average()`

**`startsWith()`**  
A String function that checks if the string begins with a given character or word.

**String Formatting**  
`"%.1f".format(value)` formats a Double to 1 decimal place.

---

## Step by Step Data Flow
```
Start:   [Alice/25, Bob/30, Charlie/35, Anna/22, Ben/28]

filter:  [Alice/25, Bob/30, Anna/22, Ben/28]   <- Charlie removed

map:     [25, 30, 22, 28]                      <- ages only

average: 26.25                                 <- (25+30+22+28)/4

output:  26.3                                  <- rounded to 1 decimal
```

---

## Expected Output
```
Average age: 26.3
```

---

## Key Takeaway

> Chaining `filter`, `map`, and `average()` on a list of data class objects  
> shows how Kotlin handles real-world data processing cleanly and concisely.  
> This pattern is extremely common in Android development.