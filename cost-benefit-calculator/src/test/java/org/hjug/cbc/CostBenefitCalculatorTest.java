package org.hjug.cbc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.*;
import java.util.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hjug.git.GitLogReader;
import org.hjug.metrics.GodClass;
import org.hjug.metrics.PMDGodClassRuleRunner;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

public class CostBenefitCalculatorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Git git;
    private Repository repository;

    @Before
    public void setUp() throws GitAPIException {
        git = Git.init().setDirectory(tempFolder.getRoot()).call();
        repository = git.getRepository();
    }

    @After
    public void tearDown() {
        repository.close();
    }

    @Test
    public void testCostBenefitCalculation() throws IOException, GitAPIException, InterruptedException {
        String attributeHandler = "AttributeHandler.java";
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(attributeHandler);
        writeFile(attributeHandler, convertInputStreamToString(resourceAsStream));

        git.add().addFilepattern(".").call();
        RevCommit firstCommit = git.commit().setMessage("message").call();

        // Sleeping for one second to guarantee commits have different time stamps
        Thread.sleep(1000);

        // write contents of updated file to original file
        InputStream resourceAsStream2 = getClass().getClassLoader().getResourceAsStream("AttributeHandler2.java");
        writeFile(attributeHandler, convertInputStreamToString(resourceAsStream2));

        InputStream resourceAsStream3 =
                getClass().getClassLoader().getResourceAsStream("AttributeHandlerAndSorter.java");
        writeFile("AttributeHandlerAndSorter.java", convertInputStreamToString(resourceAsStream3));

        git.add().addFilepattern(".").call();
        RevCommit secondCommit = git.commit().setMessage("message").call();

        CostBenefitCalculator costBenefitCalculator = new CostBenefitCalculator();
        List<RankedDisharmony> disharmonies = costBenefitCalculator.calculateCostBenefitValues(
                git.getRepository().getDirectory().getPath());

        Assert.assertEquals(0, disharmonies.get(0).getPriority().intValue());
        Assert.assertEquals(0, disharmonies.get(1).getPriority().intValue());
    }

    @Test
    public void scanClassesInRepo2() throws IOException, GitAPIException {
        String attributeHandler = "AttributeHandler.java";
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(attributeHandler);
        writeFile(attributeHandler, convertInputStreamToString(resourceAsStream));

        git.add().addFilepattern(".").call();
        git.commit().setMessage("message").call();

        GitLogReader gitLogReader = new GitLogReader();
        Map<String, ByteArrayOutputStream> filesToScan = gitLogReader.listRepositoryContentsAtHEAD(repository);

        PMDGodClassRuleRunner ruleRunner = new PMDGodClassRuleRunner();

        Map<String, GodClass> godClasses = new HashMap<>();
        for (String filePath : filesToScan.keySet()) {
            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(filesToScan.get(filePath).toByteArray());
            Optional<GodClass> godClassOptional = ruleRunner.runGodClassRule(filePath, inputStream);
            godClassOptional.ifPresent(godClass -> godClasses.put(filePath, godClass));
        }

        Assert.assertFalse(godClasses.isEmpty());
    }

    @Test
    public void scanClassesInRepo() throws IOException, GitAPIException {
        String attributeHandler = "AttributeHandler.java";
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(attributeHandler);
        writeFile(attributeHandler, convertInputStreamToString(resourceAsStream));

        git.add().addFilepattern(".").call();
        git.commit().setMessage("message").call();

        GitLogReader gitLogReader = new GitLogReader();
        Map<String, ByteArrayOutputStream> filesToScan = gitLogReader.listRepositoryContentsAtHEAD(repository);

        PMDGodClassRuleRunner ruleRunner = new PMDGodClassRuleRunner();

        Map<String, GodClass> godClasses = new HashMap<>();
        for (String filePath : filesToScan.keySet()) {
            ByteArrayInputStream inputStream =
                    new ByteArrayInputStream(filesToScan.get(filePath).toByteArray());
            Optional<GodClass> godClassOptional = ruleRunner.runGodClassRule(filePath, inputStream);
            godClassOptional.ifPresent(godClass -> godClasses.put(filePath, godClass));
        }

        Assert.assertFalse(godClasses.isEmpty());
    }

    private void writeFile(String name, String content) throws IOException {
        File file = new File(git.getRepository().getWorkTree(), name);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes(UTF_8));
        }
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }
}
