package org.example;

import com.intuit.fuzzymatcher.component.MatchService;
import com.intuit.fuzzymatcher.domain.Document;
import com.intuit.fuzzymatcher.domain.Element;
import com.intuit.fuzzymatcher.domain.ElementType;
import com.intuit.fuzzymatcher.domain.Match;
import com.opencsv.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class App {
    private static final MatchService matchService = new MatchService();
    private static final double matchScore = 0.8; // 0-0.9999

    public static final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy/MM/dd");
    public static final SimpleDateFormat defaultDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
    private static final String inputFileName = "src/main/java/org/example/sample/input.csv";
    private static final String outputFileName = "src/main/java/org/example/sample/output.csv";

    public static void main(String[] args) {
        try {
            List<Person> people = readCsvFile(inputFileName);
            Set<OutputObj> output = matchCheck(people);
            writeCsvFile(output, outputFileName);
            log.info("Hoàn tất kiểm tra trùng lặp, kết quả đã ghi ra file");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static Set<OutputObj> matchCheck(List<Person> personList) {
        List<Document> documents = personList.stream().map(p ->
                new Document.Builder(p.getPersonId())
                        .addElement(new Element.Builder<String>().setValue(p.getFullName()).setType(ElementType.NAME).createElement())
                        .addElement(new Element.Builder<Date>().setValue(p.getDateOfBirth()).setType(ElementType.DATE).createElement())
                        .addElement(new Element.Builder<String>().setValue(p.getPlaceOfBirth()).setType(ElementType.ADDRESS).createElement())
                        .addElement(new Element.Builder<String>().setValue(p.getPersonId()).setType(ElementType.PHONE).createElement())
                        .setThreshold(matchScore)
                        .createDocument()
        ).collect(Collectors.toList());

        long start, end;

        start = System.currentTimeMillis();
        Map<String, List<Match<Document>>> matchResult = matchService.applyMatchByDocId(documents);
        end = System.currentTimeMillis();
        log.info("Thời gian thực hiện kiểm tra: {}ms", end-start);

        if (matchResult.isEmpty()) {
            return null;
        }

        Set<OutputObj> result = new HashSet<>();

        matchResult.forEach((key, value) -> value.forEach(match ->
                result.add(OutputObj.builder()
                        .matchData(Person.toPerson(match.getData()).toString())
                        .matchWith(Person.toPerson(match.getMatchedWith()).toString())
                        .score(String.valueOf(match.getScore().getResult()))
                        .build()
                )
        ));

        return result;
    }

    public static List<Person> readCsvFile(String fileName) throws Exception {
        List<Person> personList = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(fileName))
                .withCSVParser(new CSVParserBuilder()
                        .withSeparator(';')
                        .build())
                .build()) {

            String[] line;
            while ((line = reader.readNext()) != null) {
                personList.add(Person.builder()
                        .fullName(line[0])
                        .dateOfBirth(inputDateFormat.parse(line[1]))
                        .placeOfBirth(line[2])
                        .personId(line[3])
                        .build()
                );
            }
        }
        return personList;
    }

    public static void writeCsvFile(Set<OutputObj> output, String fileName) throws Exception {
        if (output == null) {
            output = Collections.emptySet();
        }
        try (CSVWriter writer = (CSVWriter) new CSVWriterBuilder(new FileWriter(fileName))
                .withSeparator(';')
                .build()) {

            writer.writeNext(new String[]{"Số cặp nghi vấn trùng nhau: " + output.size()});
            writer.writeNext(new String[]{"Data", "Score"});

            output.forEach(data -> {
                String[] line1 = {data.matchData, data.score};
                String[] line2 = {data.matchWith, data.score};
                writer.writeNext(line1);
                writer.writeNext(line2);
            });
        }
    }

    @Getter
    @ToString
    @Builder
    @AllArgsConstructor
    private static class OutputObj {
        String matchData;
        String matchWith;
        String score;

        @Override
        public int hashCode() {
            int result = score.hashCode();
            if (matchData.compareTo(matchWith) < 0) {
                result = 31 * result + matchData.hashCode();
                result = 31 * result + matchWith.hashCode();
            } else {
                result = 31 * result + matchWith.hashCode();
                result = 31 * result + matchData.hashCode();
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (getClass() != obj.getClass())
                return false;
            OutputObj c = (OutputObj) obj;

            if (!score.equals(c.score)) {
                return false;
            }

            if (matchData.equals(c.matchData) && matchWith.equals(c.matchWith)) {
                return true;
            }

            if (matchData.equals(c.matchWith) && matchWith.equals(c.matchData)) {
                return true;
            }

            return false;
        }
    }
}
