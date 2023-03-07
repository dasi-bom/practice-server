package com.dasibom.practice.service;

import com.dasibom.practice.condition.DiaryReadCondition;
import com.dasibom.practice.domain.Diary;
import com.dasibom.practice.dto.DiaryBriefResDto;
import com.dasibom.practice.dto.DiaryDetailResDto;
import com.dasibom.practice.dto.DiarySaveReqDto;
import com.dasibom.practice.dto.DiaryUpdateReqDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface DiaryService {

    Long issueId();

    Diary save(Long diaryId, DiarySaveReqDto requestDto);

    DiaryDetailResDto getDetailedDiary(Long diaryId);

    Slice<DiaryBriefResDto> getDiaryList(Long cursor, DiaryReadCondition condition, Pageable pageRequest);

    void update(Long diaryId, DiaryUpdateReqDto updateRequestDto);

    Diary delete(Long diaryId);

}
