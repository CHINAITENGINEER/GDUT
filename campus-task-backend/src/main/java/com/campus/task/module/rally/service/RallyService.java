package com.campus.task.module.rally.service;

import com.campus.task.module.rally.dto.RallyCreateDTO;
import com.campus.task.module.rally.vo.RallyActivityVO;
import com.campus.task.module.rally.vo.RallyMemberVO;
import com.campus.task.module.rally.vo.RallyMessageVO;

import java.util.List;

public interface RallyService {

    RallyActivityVO create(Long organizerId, RallyCreateDTO dto);

    List<RallyActivityVO> listActive();

    RallyActivityVO join(Long rallyId, Long userId);

    RallyActivityVO quit(Long rallyId, Long userId);

    void end(Long rallyId, Long userId);

    List<RallyMemberVO> members(Long rallyId, Long userId);

    List<RallyMessageVO> history(Long rallyId, Long userId);
}
