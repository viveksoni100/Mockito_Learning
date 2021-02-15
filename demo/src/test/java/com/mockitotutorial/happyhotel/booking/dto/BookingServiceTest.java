package com.mockitotutorial.happyhotel.booking.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @InjectMocks
    private BookingService bookingService;
    @Mock
    private PaymentService paymentServiceMock;
    @Mock
    private RoomService roomServiceMock;
    @Spy
    private BookingDAO bookingDAOMock;
    @Mock
    private MailSender mailSenderMock;
    @Captor
    private ArgumentCaptor<Double> doubleCaptor;

    /*@BeforeEach
    // if we are not using @BeforeEach then we can use @Mock, @Spy annotations on fields
    void setup() {
        this.paymentServiceMock = mock(PaymentService.class);
        this.roomServiceMock = mock(RoomService.class);
        this.bookingDAOMock = spy(BookingDAO.class);    // partial mock called spies
        *//*this.bookingDAOMock = mock(BookingDAO.class);*//*
        this.mailSenderMock = mock(MailSender.class);
        this.bookingService = new BookingService(paymentServiceMock, roomServiceMock,
                bookingDAOMock, mailSenderMock); //we have provided mocks of the dependencies for BS

        //        this is called nice return values
        *//*System.out.println("List returned : " + roomServiceMock.getAvailableRooms());
        System.out.println("Object returned : " + roomServiceMock.findAvailableRoomId(null));
        System.out.println("Primitive returned : " + roomServiceMock.getRoomCount());*//*

        this.doubleCaptor = ArgumentCaptor.forClass(Double.class);

    }*/

    @Test
    void should_CountAvailablePlaces() {

        int expected = 0;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }

    //    default value demo
    @Test
    void should_CountAvailablePlaces_When_OneRoomAvailable() {

        when(this.roomServiceMock.getAvailableRooms())
                .thenReturn(Collections.singletonList(new Room("Room 1", 2)));  // 5 will fail
        int expected = 2;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }

    @Test
    void should_CountAvailablePlaces_When_MultipleRoomAvailable() {

        List<Room> rooms = Arrays.asList(new Room("Room 1", 2),
                new Room("Room 2", 5));
        when(this.roomServiceMock.getAvailableRooms())
                .thenReturn(rooms);  // 5 will fail
        int expected = 7;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }

    @Test
    void should_CountAvailablePlaces_When_CalledMultipleTimes() {

        when(this.roomServiceMock.getAvailableRooms())
                .thenReturn(Collections.singletonList(new Room("Room 1", 2))) // 5 will fail
                .thenReturn(Collections.emptyList());
        int expectedFirstCall = 2;
        int expectedSecondCall = 0;

        int actualFirst = bookingService.getAvailablePlaceCount();
        int actualSecond = bookingService.getAvailablePlaceCount();

        assertAll(
                () -> assertEquals(expectedFirstCall, actualFirst),
                () -> assertEquals(expectedSecondCall, actualSecond)
        );

        //assertEquals(expected, actual);
    }

    @Test
    void should_calculateCorrectPrice() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, false);
        double expected = 4 * 2 * 50.0;

        double actual = bookingService.calculatePrice(bookingRequest);

        assertEquals(expected, actual);
    }

    @Test
    void calculatePriceEuro() {
    }

    @Test
    void should_ThrowException_When_NoRoomAvailable() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, false);
        BookingRequest bookingRequest2 = new BookingRequest("2", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, false);
        when(this.roomServiceMock.findAvailableRoomId(bookingRequest))
                .thenThrow(BusinessException.class);

        Executable executable = () -> bookingService.makeBooking(bookingRequest2);

        assertThrows(BusinessException.class, executable);
    }

    /*what if we want to throw an exception no matter what BookingRequest
    is passed thats where mockito argument_matchers (e.g any) coming in hand*/
    @Test
    void should_NotCompleteBooking_When_PriceTooHigh() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, true);
        when(this.paymentServiceMock.pay(any(), eq(400.0)))  // anyDouble()
                .thenThrow(BusinessException.class);

        Executable executable = () -> bookingService.makeBooking(bookingRequest);

        assertThrows(BusinessException.class, executable);
    }

    //    Verify method demo
    @Test
    void should_InvokePayment_When_Prepaid() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, true);

        bookingService.makeBooking(bookingRequest); // here we want to verify that paymentService.pay(bookingRequest, price) method is really called or not

        verify(paymentServiceMock).pay(bookingRequest, 400.0);
        verify(paymentServiceMock, times(2)).pay(bookingRequest, 400.0);    // this method should invoke 2 times
        verifyNoInteractions(paymentServiceMock);   // this will ensure no more method is called from this mock
    }

    @Test
    void should_NotInvokePayment_When_NotPrepaid() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, false);

        bookingService.makeBooking(bookingRequest); // here we want to verify that paymentService.pay(bookingRequest, price) method is really called or not

        verify(paymentServiceMock, never()).pay(any(), anyDouble());
    }

    //    spice / partial mock / ie. we are going in actual class
    @Test
    void should_MakeBooking_When_InputOK() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, true);

        String bookingId = bookingService.makeBooking(bookingRequest);

        verify(bookingDAOMock).save(bookingRequest);
        System.out.println("bookingId : " + bookingId);
    }

    @Test
    void should_CancelBooking_When_InputOK() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, true);
        bookingRequest.setRoomId("1.3");
        String bookingId = "1";

        doReturn(bookingRequest).when(bookingDAOMock).get(bookingId);

        // use doThrow when method (sendBookingConfirmation) returns null, cause when().Then won't work here
        // doThrow(new BusinessException()).when(mailSenderMock).sendBookingConfirmation(any());

        bookingService.cancelBooking(bookingId);    // will return null pointer without doReturn(...)
    }

    // argument captors
    @Test
    void should_PayCorrectPrice_When_InputOK() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, true);

        bookingService.makeBooking(bookingRequest);

        verify(paymentServiceMock, times(1)).pay(eq(bookingRequest), doubleCaptor.capture());
        double capturedArg = doubleCaptor.getValue();
        assertEquals(400.0, capturedArg);
    }

    @Test
    void should_PayCorrectPrices_When_MultipleCalls() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, true);
        BookingRequest bookingRequest2 = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 02), 2, true);
        List<Double> expectedValues = Arrays.asList(400.0, 100.0);

        bookingService.makeBooking(bookingRequest);
        bookingService.makeBooking(bookingRequest2);

        verify(paymentServiceMock, times(2)).pay(any(), doubleCaptor.capture());
        List<Double> capturedArg = doubleCaptor.getAllValues();
        assertEquals(expectedValues, capturedArg);
    }

    // BDD example / Behavior Driven Development
    @Test
    void should_CountAvailablePlaces_When_OneRoomAvailable_BDD() {

        given(this.roomServiceMock.getAvailableRooms())
                .willReturn(Collections.singletonList(new Room("Room 1", 2)));  // 5 will fail

        int expected = 2;

        int actual = bookingService.getAvailablePlaceCount();

        assertEquals(expected, actual);
    }

    @Test
    void should_InvokePayment_When_Prepaid_BDD() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, true);

        bookingService.makeBooking(bookingRequest); // here we want to verify that paymentService.pay(bookingRequest, price) method is really called or not

        then(paymentServiceMock).should().pay(bookingRequest, 400.0);
    }

    // strict stubbing
    @Test
    void should_InvokePayment_When_Prepaid_SS() {

        BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                LocalDate.of(2020, 01, 05), 2, true);
        lenient().when(paymentServiceMock.pay(any(), anyDouble())).thenReturn("1");   // this will be the case of stubbing (mock is not going to used)
        // lenient() will solve the stub

        bookingService.makeBooking(bookingRequest); // here we want to verify that paymentService.pay(bookingRequest, price) method is really called or not

        // just wanna check, no exception is thrown

    }

    // test static methods using mockito 3.4*
    @Test
    void should_CalculateCorrectPrice() {

        try (MockedStatic<CurrencyConverter> converterMockedStatic = mockStatic(CurrencyConverter.class)) {
            BookingRequest bookingRequest = new BookingRequest("1", LocalDate.of(2020, 01, 01),
                    LocalDate.of(2020, 01, 05), 2, true);
            double expected = 400.0;
            converterMockedStatic.when(() -> CurrencyConverter.toEuro(anyDouble())).thenReturn(400.0);

            double actual = bookingService.calculatePriceEuro(bookingRequest);

            assertEquals(expected, actual);
        }
    }

    // Mockito Answers / thenAnswer

    @Test
    void cancelBooking() {
    }
}