package org.example;

import com.intuit.fuzzymatcher.component.MatchService;
import com.intuit.fuzzymatcher.domain.Document;
import com.intuit.fuzzymatcher.domain.Element;
import com.intuit.fuzzymatcher.domain.ElementType;
import com.intuit.fuzzymatcher.domain.Match;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class App {
    public static final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy/MM/dd");
    public static final SimpleDateFormat defaultDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
    public static final String inputFileName = "src/main/java/org/example/sample/input.csv";
    public static double matchScore = 0.99; // 0-0.9999

    public static void main(String[] args) {
        try {
            List<Person> personList = readCsvFile(inputFileName);
            matchCheck(personList);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public static void matchCheck(List<Person> personList) {
        List<Document> documentList = personList.stream().map(p ->
                new Document.Builder(p.getPersonId())
                        .addElement(new Element.Builder<String>().setValue(p.getFullName()).setType(ElementType.NAME).createElement())
                        .addElement(new Element.Builder<Date>().setValue(p.getDateOfBirth()).setType(ElementType.DATE).createElement())
                        .addElement(new Element.Builder<String>().setValue(p.getPlaceOfBirth()).setType(ElementType.ADDRESS).createElement())
                        .addElement(new Element.Builder<String>().setValue(p.getPersonId()).setType(ElementType.PHONE).createElement())
                        .setThreshold(matchScore)
                        .createDocument()).collect(Collectors.toList()
        );

        MatchService matchService = new MatchService();
        Map<String, List<Match<Document>>> result = matchService.applyMatchByDocId(documentList);

        result.forEach((key, value) -> value.forEach(match ->
                System.out.printf("Data: %s Matched With: %s Score: %s%n",
                        Person.toPerson(match.getData()),
                        Person.toPerson(match.getMatchedWith()),
                        match.getScore().getResult()
                )
        ));
    }


    public static List<Person> readCsvFile(String fileName) throws IOException, CsvValidationException, ParseException {
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
}
