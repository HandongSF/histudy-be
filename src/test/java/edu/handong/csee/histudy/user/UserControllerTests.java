package edu.handong.csee.histudy.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.handong.csee.histudy.controller.UserController;
import edu.handong.csee.histudy.controller.form.BuddyForm;
import edu.handong.csee.histudy.domain.Friendship;
import edu.handong.csee.histudy.domain.FriendshipStatus;
import edu.handong.csee.histudy.domain.User;
import edu.handong.csee.histudy.dto.FriendshipDto;
import edu.handong.csee.histudy.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    UserService userService;

    @DisplayName("스터디 친구를 등록한다")
    @Test
    public void UserControllerTests_13() throws Exception {
        // given
        User sender = User.builder()
                .sid("21700123")
                .name("sender")
                .build();
        User userA = User.builder()
                .sid("21800123")
                .name("userA")
                .build();
        User userB = User.builder()
                .sid("21900123")
                .name("userB")
                .build();

        BuddyForm form = new BuddyForm(userA.getSid(), userB.getSid());
        String content = mapper.writeValueAsString(form);
        when(userService.token2User(any()))
                .thenReturn(sender);
        when(userService.sendRequest(any(), any()))
                .thenReturn(List.of(new Friendship(sender, userA), new Friendship(sender, userB)));

        // when
        MvcResult mvcResult = mockMvc
                .perform(post("/api/user/buddy")
                        .header(HttpHeaders.AUTHORIZATION, "access_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        FriendshipDto res = mapper.readValue(mvcResult.getResponse().getContentAsString(),
                FriendshipDto.class);

        // then
        assertEquals(2, res.getRequests().size());
        assertEquals(FriendshipStatus.PENDING, res.getRequests().get(0).getStatus());
    }
}
