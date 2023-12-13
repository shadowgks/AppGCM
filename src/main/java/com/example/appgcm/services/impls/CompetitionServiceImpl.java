package com.example.appgcm.services.impls;

import com.example.appgcm.dtos.CompetitionDto;
import com.example.appgcm.dtos.HuntingDto.Requests.HuntingReqDto;
import com.example.appgcm.dtos.RegisterMemberOnCompetitionDto;
import com.example.appgcm.models.entity.Competition;
import com.example.appgcm.models.entity.Member;
import com.example.appgcm.models.entity.Ranking;
import com.example.appgcm.models.entity.embedded.MemberCompetition;
import com.example.appgcm.repositories.*;
import com.example.appgcm.services.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CompetitionServiceImpl implements CompetitionService {
    private final MemberRepository memberRepository;
    private final CompetitionRepository competitionRepository;
    private final RankingRepository rankingRepository;

    @Override
    public List<Competition> findAllCompetition() {
        List<Competition> competitionList = competitionRepository.findAll();
        if (competitionList.isEmpty()) {
            throw new IllegalArgumentException("Not Found Any Competition");
        }
        return competitionList;
    }

    @Override
    public Competition findByDateCompetition(LocalDate date) {
        Optional<Competition> competition = Optional.ofNullable(competitionRepository.findByDate(date)
                .orElseThrow(() -> new IllegalArgumentException("Not found Competition By Date " +date)));
        return competition.get();
    }

    @Override
    public Competition findByCodeCompetition(String code) {
         Optional<Competition> competition = Optional.ofNullable(competitionRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Not found Competition By Code " +code)));
        return competition.get();
    }

    @Override
    public Competition saveCompetition(CompetitionDto reqDto) {
        // Substring location
        String locationSplit = reqDto.location().substring(0,3).toLowerCase();

        // Format the date
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("-yy-MM-dd");
        String formattedDate = reqDto.date().format(outputFormatter);
        String codeDone = locationSplit.concat(formattedDate);

        // Check Time
        if(reqDto.endTime().isBefore(reqDto.startTime())){
            throw new IllegalArgumentException("Check start time and end time");
        }

        //Builder Competition
        Competition competition = Competition.builder()
                .amount(reqDto.amount())
                .code(codeDone)
                .location(reqDto.location())
                .endTime(reqDto.endTime())
                .startTime(reqDto.startTime())
                .numberOfParticipants(reqDto.numberOfParticipants())
                .date(reqDto.date())
                .build();
        return competitionRepository.save(competition);
    }

    @Override
    public void deleteCompetition(Long id) {
        Optional<Competition> competition = Optional.ofNullable(competitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Not found Competition")));
        competition.ifPresent(get -> competitionRepository.deleteById(id));
    }

    @Override
    public Competition updateCompetition(Long id, CompetitionDto reqDto) {
        Optional<Competition> competition = Optional.of(competitionRepository.findById(id))
                .orElseThrow(() -> new IllegalArgumentException("Not found this Competition!"));

        // Substring location
        String locationSplit = reqDto.location().substring(0,3).toLowerCase();

        // Format the date
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("-yy-MM-dd");
        String formattedDate = reqDto.date().format(outputFormatter);
        String codeDone = locationSplit.concat(formattedDate);

        //Builder Competition
        Competition competition1 = Competition.builder()
                .id(competition.get().getId())
                .code(codeDone)
                .amount(reqDto.amount())
                .date(reqDto.date())
                .startTime(reqDto.startTime())
                .endTime(reqDto.endTime())
                .numberOfParticipants(reqDto.numberOfParticipants())
                .location(reqDto.location())
                .build();
        return competitionRepository.save(competition1);
    }

    @Override
    public Ranking registerMember(RegisterMemberOnCompetitionDto reqDto) {
        Optional<Competition> competition = Optional.ofNullable(competitionRepository.findById(reqDto.competition_id())
                .orElseThrow(() -> new IllegalArgumentException("Sorry this competition not exists!")));
        Optional<Member> member = Optional.ofNullable(memberRepository.findById(reqDto.member_id())
                .orElseThrow(() -> new IllegalArgumentException("Sorry this member not exists!")));

        // Check if member already registered on competition
        Optional<Ranking> ranking = rankingRepository.findByMemberAndCompetition(member.get(), competition.get());
        if (ranking.isPresent()){
            throw new IllegalArgumentException("This member is already registered for this competition!");
        }

        // Check date registered before 24h in competition
        //...

        // Now create ranking
        Ranking ranking1 = Ranking.builder()
                .id(MemberCompetition.builder()
                        .competitionID(member.get().getId())
                        .memberID(competition.get().getId())
                        .build())
                .member(member.get())
                .competition(competition.get())
                .rankk(0)
                .score(0)
                .build();
        return rankingRepository.save(ranking1);
    }
}
