package com.example.libreria.service;

import com.example.libreria.dto.BookResponseDTO;
import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private BookService bookService;
    
    @Mock
    private UserService userService;
    
    @InjectMocks
    private ReservationService reservationService;

    private MockMvc mvc;
    
    private User testUser;
    private Book testBook;
    private Reservation testReservation;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");
        
        testBook = new Book();
        testBook.setExternalId(258027L);
        testBook.setTitle("The Lord of the Rings");
        testBook.setPrice(new BigDecimal("15.99"));
        testBook.setStockQuantity(10);
        testBook.setAvailableQuantity(5);
        
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setRentalDays(7);
        testReservation.setStartDate(LocalDate.now());
        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setDailyRate(new BigDecimal("15.99"));
        testReservation.setTotalFee(new BigDecimal("111.93"));
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCreateReservation_Success() throws Exception {
        // TODO: Implementar el test de creación de reserva exitosa

        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setUserId(testUser.getId());
        requestDTO.setBookExternalId(testBook.getExternalId());
        requestDTO.setRentalDays(testReservation.getRentalDays());
        requestDTO.setStartDate(testReservation.getStartDate());

        ReservationResponseDTO reservation = new ReservationResponseDTO();
        reservation.setUserId(testUser.getId());
        reservation.setBookExternalId(testBook.getExternalId());
        reservation.setRentalDays(testReservation.getRentalDays());
        reservation.setStartDate(testReservation.getStartDate());
        reservation.setExpectedReturnDate(testReservation.getExpectedReturnDate());
        reservation.setDailyRate(testReservation.getDailyRate());
        reservation.setTotalFee(testReservation.getTotalFee());
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        reservation.setCreatedAt(LocalDateTime.now());



        when(reservationService.createReservation(requestDTO)).thenReturn(reservation);

        mvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("1L"))
                .andExpect(jsonPath("$.bookExternalId").value("258027L"));

    }
    
    @Test
    void testCreateReservation_BookNotAvailable() {
        // TODO: Implementar el test de creación de reserva cuando el libro no está disponible

        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setUserId(testUser.getId());
        requestDTO.setBookExternalId(testBook.getExternalId());
        requestDTO.setRentalDays(testReservation.getRentalDays());
        requestDTO.setStartDate(testReservation.getStartDate());

        BookResponseDTO bookDto = new BookResponseDTO();
        bookDto.setExternalId(258027L);
        bookDto.setTitle("The Lord of the Rings");
        bookDto.setPrice(new BigDecimal("15.99"));
        bookDto.setAvailableQuantity(0);

        ReservationResponseDTO response = reservationService.createReservation(requestDTO);

        assertNotNull(response);
        assertNull(response.getUserId());
        assertNull(response.getBookExternalId());
        assertNull(response.getStatus());
        assertEquals(0, bookDto.getAvailableQuantity());



    }
    
    @Test
    void testReturnBook_OnTime() {
        // TODO: Implementar el test de devolución de libro en tiempo

        LocalDate expectedReturn = LocalDate.of(2025, 11, 14);
        LocalDate returnDate = expectedReturn;

        testReservation.setExpectedReturnDate(expectedReturn);
        testReservation.setRentalDays(5);
        testReservation.setDailyRate(new BigDecimal("10.00"));
        testReservation.setTotalFee(null);
        testReservation.setLateFee(null);
        testBook.setStockQuantity(10);

        ReturnBookRequestDTO returnRequest = new ReturnBookRequestDTO();
        returnRequest.setReturnDate(returnDate);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        ReservationResponseDTO response = reservationService.returnBook(1L, returnRequest);

        assertNotNull(response);
        assertEquals(returnDate, response.getActualReturnDate());

        BigDecimal expectedTotal = new BigDecimal("10.00").multiply(new BigDecimal(5));
        assertEquals(0, expectedTotal.compareTo(response.getTotalFee()));
        assertEquals(0, response.getLateFee().compareTo(BigDecimal.ZERO));


        assertEquals(Reservation.ReservationStatus.ACTIVE, testReservation.getStatus());

        assertEquals(11, testBook.getStockQuantity());
    }
    
    @Test
    void testReturnBook_Overdue() {
        // TODO: Implementar el test de devolución de libro con retraso

        LocalDate expectedReturn = LocalDate.of(2025, 11, 14);
        LocalDate returnDate = LocalDate.of(2025, 11, 17);

        testReservation.setExpectedReturnDate(expectedReturn);
        testReservation.setDailyRate(new BigDecimal("10.00"));
        testReservation.setTotalFee(null);
        testReservation.setLateFee(null);
        testBook.setPrice(new BigDecimal("20.00"));
        testBook.setStockQuantity(10);

        ReturnBookRequestDTO returnRequest = new ReturnBookRequestDTO();
        returnRequest.setReturnDate(returnDate);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        ReservationResponseDTO response = reservationService.returnBook(1L, returnRequest);

        assertNotNull(response);
        assertEquals(returnDate, response.getActualReturnDate());

        BigDecimal expectedLateFee = new BigDecimal("20.00")
                .multiply(new BigDecimal(3))
                .multiply(new BigDecimal("0.15")); // = 9.00

        assertEquals(0, expectedLateFee.compareTo(response.getLateFee()));

        assertNull(response.getTotalFee());

        assertEquals(Reservation.ReservationStatus.ACTIVE, testReservation.getStatus());

        assertEquals(11, testBook.getStockQuantity());
    }
    
    @Test
    void testGetReservationById_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        ReservationResponseDTO result = reservationService.getReservationById(1L);
        
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
    }
    
    @Test
    void testGetAllReservations() {
        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        
        when(reservationRepository.findAll()).thenReturn(Arrays.asList(testReservation, reservation2));
        
        List<ReservationResponseDTO> result = reservationService.getAllReservations();
        
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    void testGetReservationsByUserId() {
        when(reservationRepository.findByUserId(1L)).thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getReservationsByUserId(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetActiveReservations() {
        when(reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getActiveReservations();
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}

