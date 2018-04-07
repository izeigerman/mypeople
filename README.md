# MyPeople

The recommender system which for a given person generates a list of people (coworkers) with similar mindset based on their interests, location and department.

## Requirements

```
SBT >= 1.0.0
```

## How to run

1. Compile code using SBT:
```
sbt "project mypeopleUi" fastOptJS "project mypeople" compile
```
2. Specify token in the CULTURE_TOKEN environment variable:
```
export CULTURE_TOKEN="<token>"
```
3. Launch application using SBT:
```
sbt "project mypeople" run
```
4. Access the app from your browser using this link: [http://localhost:12345/mypeople-ui/target/scala-2.12/classes/index-dev.html](http://localhost:12345/mypeople-ui/target/scala-2.12/classes/index-dev.html)
5. Type in the user's name and click on the row to see the leaderboard.
