package com.rental.rental_room.billing;
import com.rental.rental_room.api.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.rental.rental_room.api.Views.*;
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class ReadingController {
    private final ReadingService readings;
    @PostMapping("/electricity-readings") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ReadingView electricity(@Valid @RequestBody Requests.ElectricityInput r) { return readings.electricity(r); }
    @GetMapping("/electricity-readings") public List<ReadingView> electricity(@RequestParam(required=false) Long roomId,
        @RequestParam(required=false) Integer month,@RequestParam(required=false) Integer year,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size) { return readings.electricity(roomId,month,year,page,size); }
    @GetMapping("/electricity-readings/{roomId}") public List<ReadingView> electricityRoom(@PathVariable Long roomId,
        @RequestParam(required=false) Integer month,@RequestParam(required=false) Integer year,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size) { return readings.electricity(roomId,month,year,page,size); }
    @PostMapping("/water-readings") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ReadingView water(@Valid @RequestBody Requests.WaterInput r) { return readings.water(r); }
    @GetMapping("/water-readings") public List<ReadingView> water(@RequestParam(required=false) Long roomId,
        @RequestParam(required=false) Integer month,@RequestParam(required=false) Integer year,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size) { return readings.water(roomId,month,year,page,size); }
    @GetMapping("/water-readings/{roomId}") public List<ReadingView> waterRoom(@PathVariable Long roomId,
        @RequestParam(required=false) Integer month,@RequestParam(required=false) Integer year,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="20") int size) { return readings.water(roomId,month,year,page,size); }
    @GetMapping("/meter-workflow") public List<Map<String,Object>> worksheet(@RequestParam int month,@RequestParam int year,
        @RequestParam(required=false) Long propertyId) { return readings.worksheet(month,year,propertyId); }
}
