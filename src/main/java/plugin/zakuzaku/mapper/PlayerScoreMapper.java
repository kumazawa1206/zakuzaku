//package plugin.zakuzaku.mapper;
//
//import java.util.List;
//import org.apache.ibatis.annotations.Insert;
//import org.apache.ibatis.annotations.Select;
//import plugin.zakuzaku.mapper.data.PlayerScore;
//
//public interface PlayerScoreMapper {
//
//  @Select("select * fromz player_score")
//  List<PlayerScore> selectList();
//
//  @Insert("insert player_score(player_name, score, difficulty, registered_at) values (#{playerName}, #{score}, #{difficulty}, now())")
//  int insert(PlayerScore playerScore);
//}