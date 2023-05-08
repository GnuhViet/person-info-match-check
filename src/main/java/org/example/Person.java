package org.example;

import com.intuit.fuzzymatcher.domain.Document;
import com.intuit.fuzzymatcher.domain.Element;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.ParseException;
import java.util.Date;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    private String fullName;
    private Date dateOfBirth;
    private String placeOfBirth;
    private String personId;

    public static Person toPerson(Document data) {
        Set<Element> elements = data.getPreProcessedElement();

        Person person = new Person();

        for (Element e : elements) {
            switch (e.getElementClassification().getElementType()){
                case PHONE:
                    person.setPersonId(e.getValue().toString());
                    break;
                case ADDRESS:
                    person.setPlaceOfBirth(e.getValue().toString());
                    break;
                case NAME:
                    person.setFullName(e.getValue().toString());
                    break;
                case DATE:
                    try {
                        person.setDateOfBirth(App.defaultDateFormat.parse(e.getValue().toString()));
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                    break;
                default:
                    break;
            }
        }
        return person;
    }

    @Override
    public String toString() {
        return fullName + "; " +
                App.inputDateFormat.format(dateOfBirth) + "; " +
                placeOfBirth + "; " +
                personId;
    }
}
