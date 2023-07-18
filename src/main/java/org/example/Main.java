package org.example;

/*
 * Click `Run` to execute the snippet below!
 */

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;


class Main {

    static class GithubService {

        private Integer getCommitsByUserAndRepo(String user, String repo) {
            return switch (repo) {
                case "org1/repo1" -> switch (user) {
                    case "user1" -> 15;
                    case "user2" -> 7;
                    case "user3" -> 4;
                    default -> 0;
                };
                case "org1/repo2" -> switch (user) {
                    case "user1" -> 5;
                    case "user2" -> 9;
                    case "user3" -> 15;
                    default -> 0;
                };
                case "org2/repo1" -> switch (user) {
                    case "user1" -> 10;
                    case "user2" -> 5;
                    case "user3" -> 6;
                    default -> 0;
                };
                case "org2/repo2" -> switch (user) {
                    case "user1" -> 15;
                    case "user2" -> 3;
                    case "user3" -> 13;
                    default -> 0;
                };
                case "org2/repo3" -> switch (user) {
                    case "user1" -> 4;
                    case "user2" -> 8;
                    case "user3" -> 7;
                    default -> 0;
                };
                default -> throw new IllegalArgumentException("Unknown repo: " + repo);
            };
        }

        public Integer commitCountSync(String repo, String user) {
            return getCommitsByUserAndRepo(user, repo);
        }

        public Map.Entry<String, Integer> commitCountSyncWithUser(String repo, String user) {
            return Map.entry(user, getCommitsByUserAndRepo(user, repo));
        }

        public CompletableFuture<Integer> commitCount(String repo, String user) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
                    return getCommitsByUserAndRepo(user, repo);
                } catch (InterruptedException | IllegalArgumentException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        public CompletableFuture<Map.Entry<String, Integer>> commitCountWithUser(String repo, String user) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000));
                    return Map.entry(user, getCommitsByUserAndRepo(user, repo));
                } catch (InterruptedException | IllegalArgumentException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static final List<Map.Entry<String, String>> ALL_VALID = List.of(
            Map.entry("org1/repo1", "user2"),
            Map.entry("org1/repo2", "user3"),
            Map.entry("org2/repo1", "user1"),
            Map.entry("org2/repo2", "user2"),
            Map.entry("org2/repo3", "user2")
    );

    private static final List<Map.Entry<String, String>> SOME_INVALID = List.of(
            Map.entry("org1/repo1", "user2"),
            Map.entry("org1/repo2", "user3"),
            Map.entry("org2/repo1", "user1"),
            Map.entry("org2/repo2", "user2"),
            Map.entry("not-a-valid-repo", "user4")
    );

    private static final GithubService githubService = new GithubService();

    public static void main(String[] args) {
        System.out.println(switch (readInput()) {
            case "Exercise0" -> Main.exercise0(ALL_VALID);
            case "Exercise1" -> Main.exercise1(ALL_VALID);
            case "Exercise2" -> Main.exercise2(SOME_INVALID, "user2");
            case "Exercise3" -> Main.adjustSerialization(Main.exercise3(SOME_INVALID));
            default -> throw new IllegalArgumentException("Unknown exercise: " + args[0]);
        });
    }

    public static String readInput() {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.next();
        scanner.close();

        return input;
    }

    private static String adjustSerialization(@NotNull Map<String, Integer> answer) {
        return answer.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private static Integer exercise0(List<Map.Entry<String, String>> userList) {
        // Exercise 1: Get the total number of commits for all users in all repos

        int totalCount = 0;
        for (Map.Entry<String, String> repoUserEntry : userList) {
            totalCount += githubService.commitCountSync(repoUserEntry.getKey(), repoUserEntry.getValue());
        }
        return totalCount;
    }

    private static Integer exercise1(List<Map.Entry<String, String>> userList) {
        // Exercise 1: Get the total number of commits for all users in all repos

//        int totalCount = 0;
//        for (Map.Entry<String, String> repoUserEntry : userList) {
//            try {
//                totalCount += githubService.commitCount(repoUserEntry.getKey(), repoUserEntry.getValue()).get();
//            } catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        return totalCount;

        return userList.parallelStream()
                .map(entry -> githubService.commitCount(entry.getKey(), entry.getValue()))
                .map(CompletableFuture::join).reduce(0, Integer::sum);
    }

    private static Integer exercise2(List<Map.Entry<String, String>> userList, String user) {
        // Exercise 2: Get the total number of commits for a specific user for all repos, but ignore any repos that don't exist

        int totalCount = 0;
        for (Map.Entry<String, String> repoUserEntry : userList) {
            try {
                if (Objects.equals(user, repoUserEntry.getValue()))
                    totalCount += githubService.commitCount(repoUserEntry.getKey(), repoUserEntry.getValue()).get();
            } catch (InterruptedException | ExecutionException | IllegalArgumentException ignored) {}
        }

        return totalCount;


//        return userList.parallelStream()
//                .filter(entry -> Objects.equals(entry.getValue(), user))
//                .map(entry -> githubService.commitCount(entry.getKey(), entry.getValue()).exceptionally(ex -> 0))
//                .map(CompletableFuture::join).reduce(0, Integer::sum);
    }

    private static Map<String, Integer> exercise3(List<Map.Entry<String, String>> userList) {
        // Exercise 3: Get the total number of commits for each user for all repos

        Map<String, Integer> totalCount = new HashMap<>();
        for (Map.Entry<String, String> repoUserEntry : userList) {
            try {
                var entry = githubService.commitCountWithUser(repoUserEntry.getKey(), repoUserEntry.getValue()).get();
                totalCount.compute(entry.getKey(), (k, v) -> v == null ? entry.getValue() : v + entry.getValue());
            } catch (InterruptedException | ExecutionException e) {
                totalCount.compute(repoUserEntry.getValue(), (k, v) -> v == null ? 0 : v);
            }
        }

        return totalCount;



//        return userList.parallelStream()
//                .map(entry -> githubService.commitCountWithUser(entry.getKey(), entry.getValue()).exceptionally(e -> Map.entry(entry.getValue(), 0)))
//                .map(CompletableFuture::join)
//                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }
}