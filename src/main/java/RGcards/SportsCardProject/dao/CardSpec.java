package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.entity.Card;
import org.springframework.data.jpa.domain.Specification;

public class CardSpec {

    public static Specification<Card> build(Card card) {
        return Specification
                .where(hasId(card.getId()))
                .and(likeYear(card.getYear()))
                .and(equalsSports(card.getSports()))
                .and(equalsPublisher(card.getPublisher()))
                .and(likeSet(card.getSet()))
                .and(likePlayer(card.getPlayer()))
                .and(equalsInsert(card.getInsert()))
                .and(equalsParallel(card.getParallel()))
                .and(equalsGrade(card.getGrade()))
                .and(likeNumbered(card.getNumbered()))
                .and(isAuto(card.getAuto()));
    }

    private static Specification<Card> hasId(int id) {
        return id == 0 ? null : (root, q, cb) -> cb.equal(root.get("id"), id);
    }

    private static Specification<Card> likeYear(String year) {
        return isEmpty(year) ? null : (root, q, cb) -> cb.like(root.get("year"), "%" + year + "%");
    }

    private static Specification<Card> equalsSports(String sports) {
        return isEmpty(sports) ? null : (root, q, cb) -> cb.equal(root.get("sports"), sports);
    }

    private static Specification<Card> equalsPublisher(String publisher) {
        return isEmpty(publisher) ? null : (root, q, cb) -> cb.equal(root.get("publisher"), publisher);
    }

    private static Specification<Card> likeSet(String set) {
        return isEmpty(set) ? null : (root, q, cb) -> cb.like(root.get("set"), "%" + set + "%");
    }

    private static Specification<Card> likePlayer(String player) {
        return isEmpty(player) ? null : (root, q, cb) -> cb.like(root.get("player"), "%" + player + "%");
    }

    private static Specification<Card> equalsInsert(String insert) {
        return isEmpty(insert) ? null : (root, q, cb) -> cb.equal(root.get("insert"), insert);
    }

    private static Specification<Card> equalsParallel(String parallel) {
        return isEmpty(parallel) ? null : (root, q, cb) -> cb.equal(root.get("parallel"), parallel);
    }

    private static Specification<Card> equalsGrade(String grade) {
        return isEmpty(grade) ? null : (root, q, cb) -> cb.equal(root.get("grade"), grade);
    }

    private static Specification<Card> likeNumbered(String numbered) {
        return isEmpty(numbered) ? null : (root, q, cb) -> cb.like(root.get("numbered"), "%" + numbered + "%");
    }

    private static Specification<Card> isAuto(Boolean auto) {
        return (auto == null || !auto) ? null : (root, q, cb) -> cb.isTrue(root.get("auto"));
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
