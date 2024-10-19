package RGcards.SportsCardProject.dao;

import RGcards.SportsCardProject.eto.Card;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@Slf4j
public class CardDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int getCardsCount() {
        String sql = "Select count(*) from cards";
        int count = jdbcTemplate.queryForObject(sql, Integer.class);
        System.out.println("Card count" + count);
        return count;
    }

    public List<Card> getCardsByPage(int page) {
        List<Card> cards = new ArrayList<>();
        StringBuilder sql = new StringBuilder("Select * from cards order by id desc limit 10 ");
        if (page > 1) sql.append("offset " + 10 * (page - 1));
        cards = jdbcTemplate.query(sql.toString(), ((rs, rowNum) -> new Card(
                        rs.getInt("id"),
                        rs.getString("year"),
                        rs.getString("publisher"),
                        rs.getString("set"),
                        rs.getString("player"),
                        rs.getBoolean("auto"),
                        rs.getString("insert"),
                        rs.getString("parallel"),
                        rs.getString("numbered"),
                        rs.getString("sports"),
                        rs.getString("grade"),
                        rs.getDouble("value"),
                        rs.getString("note")
                ))
        );

        return cards;
    }

    public List<Card> getCardsByTransactionId(int trancsactionId) {
        List<Card> cards = new ArrayList<>();
        StringBuilder sql = new StringBuilder("Select * from cards c inner join transaction_infos ti on ti.card_id = c.id " +
                " where ti.transaction_id = "+trancsactionId +" order by c.id desc");
        cards = jdbcTemplate.query(sql.toString(), ((rs, rowNum) -> new Card(
                        rs.getInt("id"),
                        rs.getString("year"),
                        rs.getString("publisher"),
                        rs.getString("set"),
                        rs.getString("player"),
                        rs.getBoolean("auto"),
                        rs.getString("insert"),
                        rs.getString("parallel"),
                        rs.getString("numbered"),
                        rs.getString("sports"),
                        rs.getString("grade"),
                        rs.getDouble("value"),
                        rs.getString("note")
                ))
        );

        return cards;
    }

    public List<Card> getCardByParams(Card paramCard) {
        List<Card> cards = new ArrayList<>();
        String sql = setCardQuery(paramCard);
        log.info(sql);
        cards = jdbcTemplate.query(sql, ((rs, rowNum) -> new Card(
                        rs.getInt("id"),
                        rs.getString("year"),
                        rs.getString("publisher"),
                        rs.getString("set"),
                        rs.getString("player"),
                        rs.getBoolean("auto"),
                        rs.getString("insert"),
                        rs.getString("parallel"),
                        rs.getString("numbered"),
                        rs.getString("sports"),
                        rs.getString("grade"),
                        rs.getDouble("value"),
                        rs.getString("note")
                ))
        );
        return cards;
    }

    public String setCardQuery(Card card) {
        StringBuilder res = new StringBuilder("Select * from cards where ");
        int paramCount = 0;
        if (card.getId() != 0) {
            res.append(" id = " + card.getId());
            paramCount++;
        }
        if (card.getYear() != null && !card.getYear().equals("")) {
            if (paramCount != 0) res.append(" and ");
            res.append(" year like '%" + card.getYear() + "%'");
            paramCount++;
        }
        if (card.getSports() != null && !card.getSports().equals("")) {
            if (paramCount != 0) res.append(" and ");
            res.append(" sports = '" + card.getSports() + "'");
            paramCount++;
        }
        if (card.getPublisher() != null && !card.getPublisher().equals("")) {
            if (paramCount != 0) res.append(" and ");
            res.append(" publisher = '" + card.getPublisher() + "'");
            paramCount++;
        }
        if (card.getSet() != null && !card.getSet().equals("")) {
            if (paramCount != 0) res.append(" and ");
            res.append(" set like '%" + card.getSet() + "%'");
            paramCount++;
        }
        if (card.getPlayer() != null && !card.getPlayer().equals("")) {
            if (paramCount != 0) res.append(" and ");
            res.append(" player like '%" + card.getPlayer() + "%'");
            paramCount++;
        }
        if (card.getInsert() != null && !card.getInsert().equals("")) {
            if (paramCount != 0) res.append(" and ");
            res.append(" insert = '" + card.getInsert() + "'");
            paramCount++;
        }
        if (card.getParallel() != null && !card.getParallel().equals("")) {
            if (paramCount != 0) res.append(" and ");
            res.append(" parallel = '" + card.getParallel() + "'");
            paramCount++;
        }
        if (card.getGrade() != null && !card.getGrade().equals("")) {
            if (paramCount != 0) res.append(" and ");
            res.append(" grade = '" + card.getGrade() + "'");
            paramCount++;
        }
        if (card.getNumbered() != null && !card.getNumbered().equals("")) {
            if (paramCount != 0) res.append(" and ");
            res.append(" numbered like '%" + card.getNumbered() + "%'");
            paramCount++;
        }
        if (card.getAuto() != null && card.getAuto()) {
            if (paramCount != 0) res.append(" and ");
            res.append(" auto = true ");
            paramCount++;
        }
        if (paramCount == 0) res.delete(20, res.length() - 1);
        res.append(" order by id desc ");

        return res.toString();
    }


}
