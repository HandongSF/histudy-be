package edu.handong.csee.histudy.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamReportDto {

    @Deprecated
    private long group;

    private Integer tag;

    private List<UserDto.UserBasic> members;

    private long totalTime;

    List<ReportDto.ReportBasic> reports;
}