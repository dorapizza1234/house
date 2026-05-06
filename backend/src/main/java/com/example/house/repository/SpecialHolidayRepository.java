  package com.example.house.repository;

  import com.example.house.domain.SpecialHoliday;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.util.List;

  public interface SpecialHolidayRepository extends JpaRepository<SpecialHoliday, Long> {
      List<SpecialHoliday> findByMonthAndDay(Integer month, Integer day);
  }
