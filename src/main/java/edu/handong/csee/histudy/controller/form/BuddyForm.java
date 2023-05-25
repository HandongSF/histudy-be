package edu.handong.csee.histudy.controller.form;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuddyForm {

    /**
     * Contains the ID of the student the user wants to study with.
     */
    private List<String> buddies;

    public BuddyForm(String... buddies) {
        this.buddies = Arrays.stream(buddies).toList();
    }
}
