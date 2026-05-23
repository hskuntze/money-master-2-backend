package br.com.kuntzedevprojects.money_master_2.services;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class BrazilianBusinessDayService {

    public boolean isEligibleYieldDate(LocalDate date, boolean businessDaysOnly, boolean useBrazilianHolidays) {
        if (date == null) {
            return false;
        }
        if (!businessDaysOnly) {
            return true;
        }
        return isBusinessDay(date, useBrazilianHolidays);
    }

    public boolean isBusinessDay(LocalDate date, boolean useBrazilianHolidays) {
        if (date == null) {
            return false;
        }
        if (isWeekend(date)) {
            return false;
        }
        return !useBrazilianHolidays || !isBrazilianNationalHoliday(date);
    }

    public LocalDate previousEligibleDate(LocalDate referenceDate, boolean businessDaysOnly, boolean useBrazilianHolidays) {
        LocalDate date = referenceDate == null ? LocalDate.now().minusDays(1) : referenceDate.minusDays(1);
        while (!isEligibleYieldDate(date, businessDaysOnly, useBrazilianHolidays)) {
            date = date.minusDays(1);
        }
        return date;
    }

    public boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public boolean isBrazilianNationalHoliday(LocalDate date) {
        Set<LocalDate> holidays = nationalHolidays(date.getYear());
        return holidays.contains(date);
    }

    private Set<LocalDate> nationalHolidays(int year) {
        Set<LocalDate> holidays = new HashSet<>();
        holidays.add(LocalDate.of(year, 1, 1));
        holidays.add(LocalDate.of(year, 4, 21));
        holidays.add(LocalDate.of(year, 5, 1));
        holidays.add(LocalDate.of(year, 9, 7));
        holidays.add(LocalDate.of(year, 10, 12));
        holidays.add(LocalDate.of(year, 11, 2));
        holidays.add(LocalDate.of(year, 11, 15));
        holidays.add(LocalDate.of(year, 11, 20));
        holidays.add(LocalDate.of(year, 12, 25));

        LocalDate easter = easterSunday(year);
        holidays.add(easter.minusDays(48)); // Carnaval - segunda-feira
        holidays.add(easter.minusDays(47)); // Carnaval - terça-feira
        holidays.add(easter.minusDays(2));  // Sexta-feira Santa
        holidays.add(easter.plusDays(60));  // Corpus Christi
        return holidays;
    }

    private LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
