package br.edu.unijorge.niteconnect.members.application;

import java.util.UUID;
import org.springframework.stereotype.Component;
import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

/** Minimal authorized member contract; excludes contact and institutional identity claims. */
@Component
public class MemberAcademicReader {
    private final AccessService access;private final MemberDirectory members;
    public MemberAcademicReader(AccessService access,MemberDirectory members) { this.access=access;this.members=members; }
    public record Snapshot(UUID id,String status,long version) {}
    public Snapshot read(UUID actor,UUID member) {
        access.requireActive(actor);if(!actor.equals(member) && !access.isAdministrator(actor)) throw ApiException.forbidden();
        var m=members.require(member);return new Snapshot(m.id(),m.status().name(),m.version());
    }
}
