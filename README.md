Hindley-Milner
==============

A Kotlin implementation of the Hindley-Milner type inference algorithm. 
All sources are located in ./src/main/kotlin, and all tests are located in ./src/test/kotlin.
Feel free to look around, but don't try to use this as a library. It is not designed to be extended by the user.
If you want to use this in a project, your best solution is to copy the sources and re-write what is needed. 

Building
--------

    gradle build     # Builds the project
    gradle test      # Runs the unit tests
    gradle run       # Runs the REPL

Supported Expressions
---------------------

Boolean

    true :: Bool
    false :: Bool

Abstraction
    
    \x -> x :: a -> a
    \x -> \y -> x :: a -> b -> a
    \x -> y :: Undefined variable y

Application
    
    \x -> \y -> x y :: (a -> b) -> a -> b
    \x -> \y -> (x y) y :: (a -> a -> b) -> a -> b
    \x -> x x :: Infinite bind a to a -> a
    true false :: Unable to unify Bool and Bool -> a

Condition

    if true then true else false :: Bool
    \x -> if x then false else true :: Bool -> Bool
    \x -> \y -> \z -> if x then y else z :: Bool -> a -> a -> a
