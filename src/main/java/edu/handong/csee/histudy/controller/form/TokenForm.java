package edu.handong.csee.histudy.controller.form;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenForm {

    private String grantType;

    private String refreshToken;
}
