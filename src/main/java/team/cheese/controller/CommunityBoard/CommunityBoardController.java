package team.cheese.controller.CommunityBoard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import team.cheese.domain.Comment.CommentDto;
import team.cheese.domain.CommunityBoard.CommunityBoardDto;
import team.cheese.domain.CommunityHeart.CommunityHeartDto;
import team.cheese.domain.ImgDto;
import team.cheese.entity.ImgFactory;
import team.cheese.service.Comment.CommentService;
import team.cheese.service.CommunityBoard.CommunityBoardService;
import team.cheese.service.CommunityHeart.CommunityHeartService;
import team.cheese.service.ImgService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.*;


@Controller
@RequestMapping(value = "/community")
public  class CommunityBoardController {
    @Autowired
    CommunityBoardService communityBoardService;

    @Autowired
    CommunityHeartService communityHeartService;

    @Autowired
    CommentService commentService;

    @Autowired
    ImgService imgService;



    //community메인페이지
    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String communityBoardHome(Model m) throws Exception {
        try {
            List<CommunityBoardDto> list = communityBoardService.readAll();
            m.addAttribute("list", list);
        } catch (Exception e) {
            e.printStackTrace();
            return "/ErrorPage";
        }
        return "/CommunityHome";

    }

    //community세부 리스트 페이지
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String communityBoardList(CommunityBoardDto communityBoardDto, Model m) throws Exception {

        List<CommunityBoardDto> list = communityBoardService.readAll();
        m.addAttribute("list", list);
        return "/CommunityList";
    }

    //community세부 리스트 페이지ajax
    @RequestMapping(value = "/story", method = RequestMethod.GET)
    @ResponseBody
    public List test(Character ur_state) throws Exception {
        List<CommunityBoardDto> list = communityBoardService.readAll();

        return list;
    }

    //글쓰기 페이지로 이동
    @RequestMapping(value = "/write", method = RequestMethod.GET)
    public String communityBoard() throws Exception {
        return "/CommunityWriteBoard";
    }


    //세션값 필요
    @ResponseBody
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String,Object>map,
                        Model m,
                        RedirectAttributes redirectAttributes,
                        HttpServletRequest request) throws Exception {

        HttpSession session = request.getSession();
        String postOwnerUser = (String) session.getAttribute("userId");


        // ObjectMapper : JSON 형태를 JAVA 객체로 변환

        ObjectMapper objectMapper = new ObjectMapper();
        CommunityBoardDto communityBoardDto = objectMapper.convertValue(map.get("communityBoardDto"), CommunityBoardDto.class);

        ArrayList<ImgDto> imgList = objectMapper.convertValue(map.get("imgList"), new TypeReference<ArrayList<ImgDto>>() {});

        if(imgList.size() != 0){
            //이미지영역
            ImgFactory ifc = new ImgFactory();
            //이미지 유효성검사 하는곳
            imgList = ifc.checkimgfile(map);
        }

        //유효성 검사를 위한임의의값
        communityBoardDto.setur_id(postOwnerUser);
        communityBoardDto.setNick("skyLee");
        communityBoardDto.setaddr_cd("11010720");
        communityBoardDto.setaddr_no(2);
        communityBoardDto.setaddr_name("서울특별시 종로구 사직동");
        //필수
        communityBoardDto.setfirst_id(postOwnerUser);
        communityBoardDto.setlast_id(postOwnerUser);

     //    유효성 검사 수행
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<CommunityBoardDto>> violations = validator.validate(communityBoardDto);

        for (ConstraintViolation<CommunityBoardDto> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            Object invalidValue = violation.getInvalidValue();
            CommunityBoardDto rootBean = violation.getRootBean();

            System.out.println("Field: " + propertyPath + " - Error: " + message);
            System.out.println("Invalid Value: " + invalidValue);
            System.out.println("Root Bean: " + rootBean);
        }

//         유효성 검사 결과 확인
        if (!violations.isEmpty()) {
           return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            Map mapDto = new HashMap();
            mapDto.put("communityBoardDto", communityBoardDto);
            mapDto.put("imgList", imgList);
            communityBoardService.write(mapDto);
            return new ResponseEntity<>("/community/list",HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("필수값을 입력해주세요",HttpStatus.BAD_REQUEST);
        }
    }


    @RequestMapping(value = "/read", method = RequestMethod.GET)
    public String read(Integer no, Model m, RedirectAttributes redirectAttributes) throws Exception {
        try {
            CommunityBoardDto communityBoardDto = communityBoardService.read(no);
            m.addAttribute("communityBoardDto", communityBoardDto);

            //이미지 지움
//            String imagePath = loadImagePath(communityBoardDto.getImg_full_rt());
//            m.addAttribute("imagePath", imagePath);
            List<ImgDto> imglist =  imgService.read(communityBoardDto.getGroup_no());
            m.addAttribute("imglist", imglist);

            //하트수
            String totalLikeCount = communityHeartService.countLike(no);
            m.addAttribute("totalLikeCount", totalLikeCount);


            //댓글수
            int totalCommentCount = communityBoardDto.getComment_count();
            m.addAttribute("totalCommentCount", totalCommentCount);


            return "/CommunityBoard";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            return "/ErrorPage";
        }


    }


    //세션값 필요
    @ResponseBody
    @RequestMapping(value = "/modify", method = RequestMethod.POST)
    public ResponseEntity<String> update(@RequestBody Map<String,Object>map, Model m, HttpServletRequest request) throws Exception {

        //Interceptor 사전에 들림
        // ObjectMapper : JSON 형태를 JAVA 객체로 변환

        ObjectMapper objectMapper = new ObjectMapper();
        CommunityBoardDto communityBoardDto = objectMapper.convertValue(map.get("communityBoardDto"), CommunityBoardDto.class);

        ArrayList<ImgDto> imgList = objectMapper.convertValue(map.get("imgList"), new TypeReference<ArrayList<ImgDto>>() {});

        if(imgList.size() != 0){
            //이미지영역
            ImgFactory ifc = new ImgFactory();
            //이미지 유효성검사 하는곳
            imgList = ifc.checkimgfile(map);
            if (imgList == null) {
                return new ResponseEntity<String>("이미지 등록 오류", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        //    유효성 검사 수행
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<CommunityBoardDto>> violations = validator.validate(communityBoardDto);

        for (ConstraintViolation<CommunityBoardDto> violation : violations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            Object invalidValue = violation.getInvalidValue();
            CommunityBoardDto rootBean = violation.getRootBean();

            System.out.println("Field: " + propertyPath + " - Error: " + message);
            System.out.println("Invalid Value: " + invalidValue);
            System.out.println("Root Bean: " + rootBean);
        }

         // 유효성 검사 결과 확인
        if (!violations.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try{
            Map mapDto = new HashMap();
            mapDto.put("communityBoardDto", communityBoardDto);
            mapDto.put("imgList", imgList);
            communityBoardService.modify(mapDto);
            return new ResponseEntity<>("/community/list",HttpStatus.OK);

        }catch (Exception e) {
           return new ResponseEntity<>("죄송합니다.글 수정에 실패했습니다.",HttpStatus.BAD_REQUEST);
        }

    }

    //세션값 필요
    //삭제(상태변경)
    @RequestMapping(value = "/userStateChange", method = RequestMethod.POST)
    public ResponseEntity<?> userStateChange(@RequestBody CommunityBoardDto communityBoardDto) throws Exception {

        try {
            int updateResult = communityBoardService.userStateChange(communityBoardDto);

            if (updateResult == 1) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "상태 변경에 성공하였습니다.");
                response.put("newState", communityBoardDto.getur_state());
                return ResponseEntity.ok(response);

            } else {
                // 정상적으로 처리되지 않은 경우, 내부 서버 오류 응답
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("상태 변경 실패");
            }
        } catch (Exception e) {
            // 예외 처리
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("처리 중 오류 발생: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/edit", method = RequestMethod.GET)
    public String Edit(Integer no, Model m, HttpServletRequest request) throws Exception {
        CommunityBoardDto communityBoardDto = communityBoardService.findCommunityBoardById(no);
        m.addAttribute("communityBoardDto", communityBoardDto);

        List<ImgDto> imglist =  imgService.read(communityBoardDto.getGroup_no());
        m.addAttribute("imglist", imglist);

        return "CommunityWriteBoard";
    }


    //하트 누를때 상태

    @RequestMapping(value = "/doLike", method = RequestMethod.PATCH)
    public ResponseEntity<Map<String, Object>> doLike(@RequestBody Map<String, Integer> requestBody, HttpSession session) throws Exception {


        String userId = (String) session.getAttribute("ur_id");
        int postNo = requestBody.get("no");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "User not authenticated"));
        }

        CommunityHeartDto communityHeartDto = new CommunityHeartDto();
        communityHeartDto.setUr_id(userId);
        communityHeartDto.setPost_no(postNo);
        communityHeartService.doLike(communityHeartDto);


        int totalLikeCount = communityBoardService.totalLike(postNo);
        Map<String, Object> response = new HashMap<>();
        response.put("totalLikeCount", totalLikeCount);
        System.out.println(response);


        return ResponseEntity.ok(response);


    }




    @PostMapping("/writeComment")
    @ResponseBody
    public ResponseEntity<List<CommentDto>> write(@RequestBody CommentDto commentDto, HttpSession session) {
        try {
            // 세션에서 ur_id와 nick 가져오기, 기본값 설정
            String ur_id = session.getAttribute("ur_id").toString();
            String nick = session.getAttribute("nick").toString();

            // DTO에 세션에서 가져온 데이터 설정
            commentDto.setUr_id(ur_id);
            commentDto.setNick(nick);

            // 최대 번호 찾기 및 예외 처리
            Integer maxNo = commentService.findMaxByPostNo(commentDto.getPost_no());
            commentDto.setNo(maxNo + 1);


            //    유효성 검사 수행
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<CommentDto>> violations = validator.validate(commentDto);

            // 댓글 작성
            commentService.write(commentDto);

            // 댓글 목록 읽기
            List<CommentDto> comments = commentService.readAll(commentDto.getPost_no());
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            // 로깅 및 에러 응답 처리

            e.printStackTrace();

        }
        return null;
    }


    @GetMapping("/comments")
    public ResponseEntity<List<CommentDto>> readComments(@RequestParam int postId) throws Exception {
        try{
            List<CommentDto> comments = commentService.readAll(postId);
            return ResponseEntity.ok(comments);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}





