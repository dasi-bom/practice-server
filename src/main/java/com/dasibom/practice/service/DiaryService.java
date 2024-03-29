package com.dasibom.practice.service;

import com.dasibom.practice.domain.Diary;
import com.dasibom.practice.domain.StampType;
import com.dasibom.practice.domain.User;
import com.dasibom.practice.dto.DiaryDto;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface DiaryService {

    Long issueId();

    Diary save(Long diaryId, DiaryDto.SaveRequest requestDto);

    DiaryDto.DetailResponse getDetailedDiary(Long diaryId);

    Slice<DiaryDto.SimpleResponse> getDiaryList(Long cursor, DiaryDto.ReadCondition condition, Pageable pageRequest);

    void update(Long diaryId, DiaryDto.UpdateRequest updateRequestDto);

    Diary deleteBeforeS3(Long diaryId);

    void deleteAfterS3(Long diaryId);

    List<DiaryDto.DetailResponse> getRecordList(StampType stampType, String petName, User user);
}
