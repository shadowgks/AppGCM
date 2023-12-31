package com.example.appgcm.services.impls;

import com.example.appgcm.dtos.HuntingDto.Requests.HuntingReqDto;
import com.example.appgcm.models.entity.*;
import com.example.appgcm.models.entity.embedded.MemberCompetition;
import com.example.appgcm.repositories.*;
import com.example.appgcm.services.HuntingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HuntingServiceImpl implements HuntingService {
    private final HuntingRepository huntingRepository;
    private final FishRepository fishRepository;
    private final MemberRepository memberRepository;
    private final CompetitionRepository competitionRepository;
    private final RankingRepository rankingRepository;

    @Override
    public Hunting sumHuntingFish(HuntingReqDto huntingDto) {
        // Check if exists
        Optional<Fish> fish = Optional.ofNullable(fishRepository.findByName(huntingDto.fishName())
                .orElseThrow(() -> new IllegalArgumentException("Sorry this fish not exists!")));
        Optional<Competition> competition = Optional.ofNullable(competitionRepository.findByCode(huntingDto.competitionCode())
                .orElseThrow(() -> new IllegalArgumentException("Sorry this competition not exists!")));
        Optional<Member> member = Optional.ofNullable(memberRepository.findByIdentityNumber(huntingDto.memberIdentity())
                .orElseThrow(() -> new IllegalArgumentException("Sorry this member not exists!")));
        Optional<Ranking> getMemberAndCompetition = Optional.ofNullable(rankingRepository.findByMemberAndCompetition(member.get(), competition.get())
                .orElseThrow(() -> new IllegalArgumentException("Sorry this member does not register yet in the competition!")));

        // Check average weight
        if (huntingDto.weight() < fish.get().getAverageWeight()){
            throw new IllegalArgumentException("The weight of the hunted fish exceeds the average weight!");
        }

        // Check date for hunting
        if(competition.get().getDate().isBefore(LocalDate.now())){
            throw new IllegalArgumentException("You can not hunting in this competition because date of competition is expired");
        }

        // Check date for hunting
        checkDate(competition.get());

        // Check CompetitionAndMemberAndFish if exists | Update Hunting
        Integer scoreMemberInRanking = getMemberAndCompetition.get().getScore();
        Integer getPointFish = fish.get().getLevel().getPoints();
        Optional<Hunting> findByCompetitionAndMemberAndFish = huntingRepository.findByCompetitionAndMemberAndFish(competition.get(), member.get(), fish.get());
        if (findByCompetitionAndMemberAndFish.isPresent()){
            Hunting huntingData = findByCompetitionAndMemberAndFish.get();
            Hunting updateHunting = Hunting.builder()
                    .id(huntingData.getId())
                    .fish(huntingData.getFish())
                    .numberOfFish(huntingData.getNumberOfFish() + 1)
                    .competition(huntingData.getCompetition())
                    .member(huntingData.getMember())
                    .build();

            // Create Score Member in ranking
            Integer getNumberFishMemberOnHunting = huntingData.getNumberOfFish();
            Ranking ranking1 = Ranking.builder()
                    .id(MemberCompetition.builder()
                            .memberID(member.get().getId())
                            .competitionID(competition.get().getId())
                            .build())
                    .score(getPointFish + scoreMemberInRanking)
                    .competition(competition.get())
                    .member(member.get())
                    .build();
            rankingRepository.save(ranking1);
            return huntingRepository.save(updateHunting);
        }

        // Create Hunting
        Hunting createHunting = Hunting.builder()
                .fish(fish.get())
                .competition(competition.get())
                .member(member.get())
                .numberOfFish(1)
                .build();

        // Create Ranking
        Ranking ranking1 = Ranking.builder()
                .id(MemberCompetition.builder()
                        .memberID(member.get().getId())
                        .competitionID(competition.get().getId())
                        .build())
                .score(getPointFish + scoreMemberInRanking)
                .competition(competition.get())
                .member(member.get())
                .build();
        rankingRepository.save(ranking1);
        return huntingRepository.save(createHunting);
    }


    public void checkDate(Competition competition){
        LocalDateTime dateNow = LocalDateTime.now();
        LocalDateTime dateCompetition = LocalDateTime.of(competition.getDate(), competition.getStartTime());

        if(dateNow.isBefore(dateCompetition)){
            throw new IllegalArgumentException("This competition doesn't started yet!");
        }
    }
}
