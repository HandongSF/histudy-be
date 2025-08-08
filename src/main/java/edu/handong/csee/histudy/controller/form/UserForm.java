package edu.handong.csee.histudy.controller.form;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserForm {

    private String sub;

    private String name;

    private String email;

    private String sid; // Student ID
}
