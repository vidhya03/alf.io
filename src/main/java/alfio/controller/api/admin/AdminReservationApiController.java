/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.controller.api.admin;

import alfio.manager.AdminReservationManager;
import alfio.manager.EventManager;
import alfio.manager.TicketReservationManager;
import alfio.model.*;
import alfio.model.modification.AdminReservationModification;
import alfio.model.result.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RequestMapping("/admin/api/reservation")
@RestController
public class AdminReservationApiController {
    private final AdminReservationManager adminReservationManager;
    private final EventManager eventManager;
    private final TicketReservationManager ticketReservationManager;

    @Autowired
    public AdminReservationApiController(AdminReservationManager adminReservationManager,
                                         EventManager eventManager,
                                         TicketReservationManager ticketReservationManager) {
        this.adminReservationManager = adminReservationManager;
        this.eventManager = eventManager;
        this.ticketReservationManager = ticketReservationManager;
    }

    @RequestMapping(value = "/event/{eventName}/new", method = RequestMethod.POST)
    public Result<String> createNew(@PathVariable("eventName") String eventName, @RequestBody AdminReservationModification reservation, Principal principal) {
        return adminReservationManager.createReservation(reservation, eventName, principal.getName()).map(r -> r.getLeft().getId());
    }

    @RequestMapping(value = "/event/{eventName}/{reservationId}/confirm", method = RequestMethod.PUT)
    public Result<TicketReservationDescriptor> confirmReservation(@PathVariable("eventName") String eventName, @PathVariable("reservationId") String reservationId, Principal principal) {
        return adminReservationManager.confirmReservation(eventName, reservationId, principal.getName())
            .map(triple -> toReservationDescriptor(reservationId, triple));
    }

    @RequestMapping(value = "/event/{eventName}/{reservationId}", method = RequestMethod.POST)
    public Result<Boolean> updateReservation(@PathVariable("eventName") String eventName, @PathVariable("reservationId") String reservationId,
                                             @RequestBody AdminReservationModification arm, Principal principal) {
        return adminReservationManager.updateReservation(eventName, reservationId, arm, principal.getName());
    }

    @RequestMapping(value = "/event/{eventName}/{reservationId}/notify", method = RequestMethod.PUT)
    public Result<Boolean> notifyReservation(@PathVariable("eventName") String eventName, @PathVariable("reservationId") String reservationId,
                                             @RequestBody AdminReservationModification arm, Principal principal) {
        return adminReservationManager.notify(eventName, reservationId, arm, principal.getName());
    }

    @RequestMapping(value = "/event/{eventName}/{reservationId}", method = RequestMethod.GET)
    public Result<TicketReservationDescriptor> loadReservation(@PathVariable("eventName") String eventName, @PathVariable("reservationId") String reservationId, Principal principal) {
        return adminReservationManager.loadReservation(eventName, reservationId, principal.getName())
            .map(triple -> toReservationDescriptor(reservationId, triple));
    }

    private TicketReservationDescriptor toReservationDescriptor(String reservationId, Triple<TicketReservation, List<Ticket>, Event> triple) {
        List<SerializablePair<TicketCategory, List<Ticket>>> tickets = triple.getMiddle().stream().collect(Collectors.groupingBy(Ticket::getCategoryId)).entrySet().stream()
            .map(entry -> SerializablePair.of(eventManager.getTicketCategoryById(entry.getKey(), triple.getRight().getId()), entry.getValue()))
            .collect(Collectors.toList());
        TicketReservation reservation = triple.getLeft();
        return new TicketReservationDescriptor(reservation, ticketReservationManager.orderSummaryForReservationId(reservationId, triple.getRight(), Locale.forLanguageTag(reservation.getUserLanguage())), tickets);
    }

    @RequiredArgsConstructor
    @Getter
    public static class TicketReservationDescriptor {
        private final TicketReservation reservation;
        private final OrderSummary orderSummary;
        private final List<SerializablePair<TicketCategory, List<Ticket>>> ticketsByCategory;
    }
}
