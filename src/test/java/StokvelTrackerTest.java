import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StokvelTrackerTest {

    private static final String MEMBERS_PROMPT = "Members (2-12): ";
    private static final String CONTRIBUTION_PROMPT = "Monthly contribution in rand (50-5000): ";
    private static final String MONTHS_PROMPT = "Months (1-24): ";

    private static String run(String typed) throws Exception {
        return ConsoleRunner.run(StokvelTracker::main, typed);
    }

    /** The printed lines with the three prompts taken out, so only messages and output are left. */
    private static List<String> lines(String output) {
        String rest = output.replace(MEMBERS_PROMPT, "")
                .replace(CONTRIBUTION_PROMPT, "")
                .replace(MONTHS_PROMPT, "");
        return rest.isEmpty() ? List.of() : List.of(rest.split("\n", -1));
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    @Test
    @DisplayName("The example run: 3 members, R500 a month, 4 months")
    void exampleRun() throws Exception {
        String output = run("3\n500\n4\n");
        assertTrue(output.startsWith(MEMBERS_PROMPT + CONTRIBUTION_PROMPT + MONTHS_PROMPT),
                "Print the three prompts with print (not println), in this order: " + MEMBERS_PROMPT
                        + CONTRIBUTION_PROMPT + MONTHS_PROMPT);
        assertEquals(List.of(
                "Month | Receives  | Pot",
                "    1 | Member 1  | R1500",
                "    2 | Member 2  | R1500",
                "    3 | Member 3  | R1500",
                "    4 | Member 1  | R1500",
                "",
                "Member 1: paid R2000, received R3000",
                "Member 2: paid R2000, received R1500",
                "Member 3: paid R2000, received R1500",
                "Members still waiting for a payout: 0",
                ""), lines(output),
                "Compare your output with the example run in the brief, line by line.");
    }

    @Test
    @DisplayName("One row per month, under the header")
    void oneRowPerMonth() throws Exception {
        List<String> lines = lines(run("4\n100\n6\n"));
        assertEquals("Month | Receives  | Pot", lines.get(0), "The table starts with this header line.");
        assertEquals("    1 | Member 1  | R400", lines.get(1),
                "Use printf(\"%5d | Member %-2d | R%d%n\", month, receiver, pot) for each row.");
        assertEquals("    6 | Member 2  | R400", lines.get(6), "6 months means 6 rows, from month 1 to month 6.");
        assertEquals("", lines.get(7), "After the last month, print one blank line before the member summary.");
    }

    @Test
    @DisplayName("The pot rotates back to member 1 after the last member")
    void rotationWraps() throws Exception {
        List<String> lines = lines(run("3\n200\n7\n"));
        assertEquals("    4 | Member 1  | R600", lines.get(4),
                "With 3 members, month 4 starts a new round at member 1. (month - 1) % members + 1 does this.");
        assertEquals("    6 | Member 3  | R600", lines.get(6), "Month 6 is member 3's second turn.");
        assertEquals("    7 | Member 1  | R600", lines.get(7), "Month 7 is member 1's third turn.");
    }

    @Test
    @DisplayName("The pot is every member's contribution, and 2-digit numbers line up")
    void potAndAlignment() throws Exception {
        List<String> lines = lines(run("12\n5000\n12\n"));
        assertEquals("    1 | Member 1  | R60000", lines.get(1),
                "12 members x R5000 = R60000. Everyone pays, including the member who receives.");
        assertEquals("   10 | Member 10 | R60000", lines.get(10),
                "%5d and %-2d keep the columns lined up when the numbers have 2 digits.");
    }

    @Test
    @DisplayName("After full rounds, everyone has received what they paid")
    void fullRoundsAreFair() throws Exception {
        List<String> lines = lines(run("4\n250\n8\n"));
        for (int member = 1; member <= 4; member++) {
            assertEquals("Member " + member + ": paid R2000, received R2000", lines.get(9 + member),
                    "8 months x R250 = R2000 paid. 4 members over 8 months means 2 turns each: 2 x R1000.");
        }
        assertEquals("Members still waiting for a payout: 0", lines.get(14));
    }

    @Test
    @DisplayName("A short stokvel leaves some members still waiting")
    void someStillWaiting() throws Exception {
        List<String> lines = lines(run("5\n200\n2\n"));
        assertEquals(List.of(
                "Member 1: paid R400, received R1000",
                "Member 2: paid R400, received R1000",
                "Member 3: paid R400, received R0",
                "Member 4: paid R400, received R0",
                "Member 5: paid R400, received R0",
                "Members still waiting for a payout: 3"), lines.subList(4, 10),
                "Only members 1 and 2 got a turn in 2 months. Members 3 to 5 have received R0, and count as waiting.");
    }

    @Test
    @DisplayName("Keeps asking until the number of members is 2 to 12")
    void validatesMembers() throws Exception {
        String output = run("1\n13\n3\n500\n4\n");
        List<String> lines = lines(output);
        assertEquals("Members must be from 2 to 12.", lines.get(0), "1 is too few members.");
        assertEquals("Members must be from 2 to 12.", lines.get(1), "13 is too many members.");
        assertEquals("Month | Receives  | Pot", lines.get(2), "3 is valid, so the program carries on.");
        assertEquals(3, count(output, MEMBERS_PROMPT),
                "After each error message, print the members prompt again and read a new number.");
    }

    @Test
    @DisplayName("Keeps asking until the contribution is R50 to R5000")
    void validatesContribution() throws Exception {
        String output = run("3\n49\n5001\n500\n4\n");
        List<String> lines = lines(output);
        assertEquals("Contribution must be from R50 to R5000.", lines.get(0), "R49 is below the minimum.");
        assertEquals("Contribution must be from R50 to R5000.", lines.get(1), "R5001 is above the maximum.");
        assertEquals("    1 | Member 1  | R1500", lines.get(3), "R500 is valid, so the pot is 3 x R500.");
        assertEquals(3, count(output, CONTRIBUTION_PROMPT),
                "After each error message, print the contribution prompt again and read a new number.");
    }

    @Test
    @DisplayName("Keeps asking until the number of months is 1 to 24")
    void validatesMonths() throws Exception {
        String output = run("3\n500\n0\n25\n4\n");
        List<String> lines = lines(output);
        assertEquals("Months must be from 1 to 24.", lines.get(0), "0 months is too few.");
        assertEquals("Months must be from 1 to 24.", lines.get(1), "25 months is too many.");
        assertEquals("    4 | Member 1  | R1500", lines.get(6), "4 is valid, so there are 4 rows.");
        assertEquals(3, count(output, MONTHS_PROMPT),
                "After each error message, print the months prompt again and read a new number.");
    }

    @Test
    @DisplayName("Accepts every value exactly on a boundary")
    void boundariesAreValid() throws Exception {
        String smallest = run("2\n50\n1\n");
        assertFalse(smallest.contains("must be"),
                "2 members, R50 and 1 month are all allowed. Check you used <= and >= correctly.");
        assertEquals(List.of(
                "Month | Receives  | Pot",
                "    1 | Member 1  | R100",
                "",
                "Member 1: paid R50, received R100",
                "Member 2: paid R50, received R0",
                "Members still waiting for a payout: 1",
                ""), lines(smallest));
        String largest = run("12\n5000\n24\n");
        assertFalse(largest.contains("must be"), "12 members, R5000 and 24 months are all allowed.");
        assertEquals("   24 | Member 12 | R60000", lines(largest).get(24),
                "Month 24 with 12 members is member 12's second turn.");
    }
}
