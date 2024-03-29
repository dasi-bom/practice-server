package com.dasibom.practice.service;

import static com.dasibom.practice.exception.ErrorCode.DIARY_ALREADY_EXIST_ERROR;
import static com.dasibom.practice.exception.ErrorCode.DIARY_NOT_FOUND;
import static com.dasibom.practice.exception.ErrorCode.PET_NOT_FOUND;
import static com.dasibom.practice.exception.ErrorCode.RECORD_NOT_FOUND;
import static com.dasibom.practice.exception.ErrorCode.STAMP_LIST_SIZE_ERROR;
import static com.dasibom.practice.exception.ErrorCode.STAMP_NOT_FOUND;
import static com.dasibom.practice.exception.ErrorCode.USER_NOT_FOUND;

import com.dasibom.practice.domain.Diary;
import com.dasibom.practice.domain.DiaryStamp;
import com.dasibom.practice.domain.Pet;
import com.dasibom.practice.domain.Record;
import com.dasibom.practice.domain.Stamp;
import com.dasibom.practice.domain.StampType;
import com.dasibom.practice.domain.User;
import com.dasibom.practice.dto.DiaryDto;
import com.dasibom.practice.exception.CustomException;
import com.dasibom.practice.repository.DiaryRepository;
import com.dasibom.practice.repository.PetRepository;
import com.dasibom.practice.repository.RecordRepository;
import com.dasibom.practice.repository.StampRepository;
import com.dasibom.practice.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final DiaryRepository diaryRepository;
    private final StampRepository stampRepository;

    private final RecordRepository recordRepository;

    // diary ID 값 발급
    public Long issueId() {
        Diary lastDiary = diaryRepository.findFirstByOrderByIdDesc();
        if (lastDiary == null) {
            return (long) 1;
        }
        return (long) (lastDiary.getId() + 1);
    }

    @Override
    @Transactional
    public Diary save(Long diaryId, DiaryDto.SaveRequest requestDto) {

        // TODO: 하드 코딩 변경
        User user = userRepository.findByUsername("test")
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        Optional<Diary> diary = diaryRepository.findById(diaryId);
        if (diary.isPresent()) {
            throw new CustomException(DIARY_ALREADY_EXIST_ERROR);
        }

        int stampListSize = 3;
        List<Stamp> stamps = extractStamps(requestDto.getStamps());
        if (stamps.size() > stampListSize) {
            throw new CustomException(STAMP_LIST_SIZE_ERROR);
        }

        List<DiaryStamp> diaryStamps = makeDiaryStamps(stamps);
        Pet pet = petRepository.findByPetNameAndOwner(requestDto.getPet().getPetName(), user)
                .orElseThrow(() -> new CustomException(PET_NOT_FOUND));
        return saveDiary(diaryId, requestDto, user, diaryStamps, pet);
    }

    @Override
    @Transactional
    public DiaryDto.DetailResponse getDetailedDiary(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(DIARY_NOT_FOUND));
        if (diary.getIsDeleted()) {
            throw new CustomException(DIARY_NOT_FOUND);
        }
        return new DiaryDto.DetailResponse(diary);
    }

    @Override
    @Transactional
    public Slice<DiaryDto.SimpleResponse> getDiaryList(Long cursor, DiaryDto.ReadCondition condition, Pageable pageRequest) {
        return diaryRepository.getDiaryBriefInfoScroll(cursor, condition, pageRequest);
    }

    @Override
    @Transactional
    public void update(Long diaryId, DiaryDto.UpdateRequest updateRequestDto) {

        // TODO: 하드 코딩 변경
        User user = userRepository.findByUsername("test")
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(DIARY_NOT_FOUND));
        if (diary.getIsDeleted()) {
            throw new CustomException(DIARY_NOT_FOUND);
        }

        Pet pet = findPet(updateRequestDto.getPet(), user);
        List<DiaryStamp> newDiaryStamps = updateStamp(updateRequestDto, diary);
        diary.updateDiary(updateRequestDto.getTitle(), updateRequestDto.getContent(), newDiaryStamps, pet);
    }

    @Override
    @Transactional
    // 게시글 삭제 시, S3 Bucket 에서 파일 제거하기 전 로직
    public Diary deleteBeforeS3(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(DIARY_NOT_FOUND));
        if (diary.getIsDeleted()) {
            throw new CustomException(DIARY_NOT_FOUND);
        }

        // 스탬프 제거
        List<DiaryStamp> diaryStamps = diary.getDiaryStamps();
        if (!diaryStamps.isEmpty()) {
            DiaryStamp.removeDiaryStamp(diaryStamps); // 해당 게시글의 DiaryStamp 목록에서 diaryStamp 삭제
        }

        return diary;
    }

    @Override
    @Transactional
    // 게시글 삭제 시, S3 Bucket 에서 파일 제거하고 난 후 로직
    public void deleteAfterS3(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(DIARY_NOT_FOUND));
        if (diary.getIsDeleted()) {
            throw new CustomException(DIARY_NOT_FOUND);
        }

        // 게시글 삭제일 업데이트
        diary.deleteDiary();
        diaryRepository.save(diary);
    }

    @Override
    @Transactional
    public List<DiaryDto.DetailResponse> getRecordList(StampType stampType, String petName, User user) {
        Pet pet = petRepository.findByPetNameAndOwner(petName, user)
                .orElseThrow(() -> new CustomException(PET_NOT_FOUND));
        List<DiaryDto.DetailResponse> result = new ArrayList<>();
        Record record = recordRepository.findByPetAndStampType(pet, stampType)
                .orElseThrow(() -> new CustomException(RECORD_NOT_FOUND));

        for (Diary diary : record.getDiaries()) {
            result.add(new DiaryDto.DetailResponse(diary));
        }
        return result;
    }

    // 누구의 일기인가요? 변경
    private Pet findPet(Pet reqPet, User user) {
        Pet pet = null;
        if (reqPet != null) {
            pet = petRepository.findByPetNameAndOwner(reqPet.getPetName(), user)
                    .orElseThrow(() -> new CustomException(PET_NOT_FOUND));
        }
        return pet;
    }

    private List<DiaryStamp> updateStamp(DiaryDto.UpdateRequest updateRequestDto, Diary diary) {
        // initialize oldStamps
        List<DiaryStamp> oldDiaryStamps = diary.getDiaryStamps();
        List<Stamp> oldStamps = new ArrayList<>();
        for (DiaryStamp oldDiaryStamp : oldDiaryStamps) {
            oldStamps.add(oldDiaryStamp.getStamp());
        }

        // 기존 스탬프 제거
        if (updateRequestDto.getStamps() != null && !oldDiaryStamps.isEmpty()) {
            DiaryStamp.removeDiaryStamp(oldDiaryStamps);
            stampRepository.deleteAll(oldStamps);
        }

        // 새로운 스탬프 생성
        int stampListSize = 3;
        List<DiaryStamp> newDiaryStamps = null;
        if (updateRequestDto.getStamps() != null) {
            if (updateRequestDto.getStamps().size() > stampListSize) {
                throw new CustomException(STAMP_LIST_SIZE_ERROR);
            }
            List<Stamp> newStamps = extractStamps(updateRequestDto.getStamps());
            newDiaryStamps = makeDiaryStamps(newStamps);
        }
        return newDiaryStamps;
    }

    private Diary saveDiary(Long diaryId, DiaryDto.SaveRequest requestDto, User user, List<DiaryStamp> diaryStamps, Pet pet) {
        Diary diary = Diary.createDiary(diaryId, user, pet, requestDto.getTitle(), requestDto.getContent(),
                diaryStamps);
        diaryRepository.save(diary);
        return diary;
    }

    private List<DiaryStamp> makeDiaryStamps(List<Stamp> stamps) {
        List<DiaryStamp> diaryStamps = new ArrayList<>();
        for (Stamp stamp : stamps) {
            DiaryStamp diarystamp = DiaryStamp.createDiaryStamp(stamp);
            diaryStamps.add(diarystamp);
        }
        return diaryStamps;
    }

    private List<Stamp> extractStamps(List<Stamp> stamps) {
        List<Stamp> resStamps = new ArrayList<>();
        for (Stamp stamp : stamps) {
            Stamp byStampType = stampRepository.findByStampType(stamp.getStampType())
                    .orElseThrow(() -> new CustomException(STAMP_NOT_FOUND));
            resStamps.add(byStampType);
        }
        return resStamps;
    }

}
