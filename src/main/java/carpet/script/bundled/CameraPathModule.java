package carpet.script.bundled;

public class CameraPathModule implements ModuleInterface
{
    @Override
    public String getName()
    {
        return "camera";
    }

    @Override
    public String getCode()
    {
        return  "$ start_path() -> \n" +
                "$(\n" +
                "$\tp = player();\n" +
                "$\tglobal_points = l(l(l(p~'x',p~'y',p~'z',p~'pitch',p~'yaw'),0,'sharp'));\n" +
                "$\tundef('global_path_precalculated');\n" +
                "$\tstr('Started path at %.1f %.1f %.1f', p~'x', p~'y', p~'z')\n" +
                "$);\n" +
                "$\n" +
                "$ add_point(delay) -> \n" +
                "$( \n" +
                "$\tp = player(); \n" +
                "$\tmode = 'sharp';\n" +
                "$\t'mode is currently unused, run_path does always sharp, gauss interpolator is always smooth';\n" +
                "$\t' but this option could be used could be used at some point by more robust interpolators';\n" +
                "$\tvector = l(p~'x',p~'y',p~'z', p~'pitch', p~'yaw');\n" +
                "$\t__add_path_segment(vector, delay, mode);\n" +
                "$\tstr('Added point %d: %.1f %.1f %.1f', length(global_points), p~'x', p~'y', p~'z')\n" +
                "$);\n" +
                "$\n" +
                "$ __add_path_segment(vector, duration, mode) -> \n" +
                "$(\n" +
                "$\tundef('global_path_precalculated');\n" +
                "$\tif ( (l('sharp','smooth') ~ mode) == null, exit('use smooth or sharp point'));\t\n" +
                "$\tif(!global_points, exit('Cannot add point to path that didn\\'t started yet!'));\n" +
                "$\tl(v, end_time, m) = element(global_points, -1);\n" +
                "$   put(vector,-1, __adjusted_rot(element(v, -1), element(vector, -1)));\n" +
                "$\tglobal_points += l(vector, end_time+duration, mode)\n" +
                "$);\n" +
                "$\n" +
                "$'adjusts current rotation so we don\\'t spin around like crazy';\n" +
                "$ __adjusted_rot(previous_rot, current_rot) -> \n" +
                "$(\n" +
                "$\twhile( abs(previous_rot-current_rot) > 180, 1000,\n" +
                "$\t\tcurrent_rot += if(previous_rot < current_rot, -360, 360)\n" +
                "$\t);\n" +
                "$\tcurrent_rot\n" +
                "$);\n" +
                "$\n" +
                "$\n" +
                "$ loop_path(times, last_section_duration) -> \n" +
                "$(\n" +
                "$\tundef('global_path_precalculated');\n" +
                "$\tpositions = map(global_points, k = element(_, 0));\n" +
                "$\tmodes = map(global_points, element(_, -1));\n" +
                "$\tdurations = map(global_points, element(element(global_points, _i+1), 1)-element(_, 1));\n" +
                "$\tput(durations, -1, last_section_duration);\n" +
                "$\tloop(times,\n" +
                "$\t\tloop( length(positions),\n" +
                "$\t\t\t__add_path_segment(element(positions, _), element(durations, _), element(modes, _))\n" +
                "$\t\t)\n" +
                "$\t);\n" +
                "$\tstr('Add %d points %d times', length(positions), times)\n" +
                "$);\n" +
                "$\n" +
                "$ adjust_speed_percentage(percentage) ->\n" +
                "$(\n" +
                "$\tundef('global_path_precalculated');\n" +
                "$\tif (percentage < 25 || percentage > 400, \n" +
                "$\t\texit('path speed can only be speed, or slowed down 4 times. Recall command for larger changes')\n" +
                "$\t);\n" +
                "$\tratio = percentage/100;\n" +
                "$\tprevious_path_length = element(element(global_points, -1),1);\n" +
                "$\tfor(global_points, put(_, 1, element(_, 1)*ratio ) );\n" +
                "$\tundef('global_path_precalculated');\n" +
                "$\tstr('path %d from %d to %d ticks',\n" +
                "$\t\tif(ratio<1,'shortened','extended'),\n" +
                "$\t\tprevious_path_length,\n" +
                "$\t\telement(element(global_points, -1),1)\n" +
                "$\t)\n" +
                "$);\n" +
                "$\n" +
                "$ select_interpolation(method) ->\n" +
                "$(\n" +
                "$\tundef('global_path_precalculated');\n" +
                "$\t__prepare_path_if_needed() -> __prepare_path_if_needed_generic();\n" +
                "$\t'each supported method needs to specify its __find_position_for_point to trace the path';\n" +
                "$\t'accepting segment number, and position in the segment';\n" +
                "$\t'or optionally __prepare_path_if_needed, if path is inefficient to compute point by point';\n" +
                "$\tif (\n" +
                "$\t\tmethod == 'linear',\n" +
                "$\t\t(\n" +
                "$\t\t\t__find_position_for_point(s, p) -> __find_position_for_linear(s, p)\n" +
                "$\t\t),\n" +
                "$\t\tmethod ~ '^gauss_',\n" +
                "$\t\t(\n" +
                "$\t\t\ttype = method - 'gauss_';\n" +
                "$\t\t\tglobal_interpol_option = if(type=='auto',0,number(type));\n" +
                "$\t\t\t__find_position_for_point(s, p) -> __find_position_for_gauss(s, p)\n" +
                "$\t\t),\n" +
                "$\t\tmethod ~ '^bungee_',\n" +
                "$\t\t(\n" +
                "$\t\t\texit('unsupported / planned');\n" +
                "$\t\t\ttype = method - 'bungee_';\n" +
                "$\t\t\tglobal_interpol_option = if(type=='auto',80,number(type));\n" +
                "$\t\t\t__prepare_path_if_needed() -> __prepare_path_if_needed_bungee()\n" +
                "$\t\t),\n" +
                "$\t\t\n" +
                "$\t\texit('Choose one of the following methods: linear, gauss:auto, gauss:<deviation>')\n" +
                "$\t);\n" +
                "$\t'Ok'\n" +
                "$);\n" +
                "$\n" +
                "$ select_interpolation('linear');\n" +
                "$\n" +
                "$ __assert_valid_for_motion() ->\n" +
                "$(\n" +
                "$\tif(!global_points, exit('Path not defined yet'));\n" +
                "$\tif(length(global_points)<2, exit('Path not complete - add more points'));\n" +
                "$\tnull\t\n" +
                "$);\n" +
                "$\n" +
                "$ __get_path_at(segment, start, index) ->\n" +
                "$(\n" +
                "$\tv = element(global_path_precalculated, start+index);\n" +
                "$\tif(v == null,\n" +
                "$\t\tv = __find_position_for_point(segment, index);\n" +
                "$\t\tput(global_path_precalculated, start+index, v)\n" +
                "\t);\n" +
                "\tv\n" +
                "$);\n" +
                "$\n" +
                "$ __invalidate_points_cache() -> global_path_precalculated = map(range(element(element(global_points, -1),1)), null);\n" +
                "$\n" +
                "$ show_path() -> \n" +
                "$(\n" +
                "$\tloop(100,\n" +
                "$\t\t_show_path_tick('dust 0.9 0.1 0.1 1', 100);\n" +
                "$\t\tgame_tick(50)\n" +
                "$\t);\n" +
                "$\t'Done!'\n" +
                "$);\n" +
                "$\n" +
                "$\n" +
                "$ run_path(fps) -> \n" +
                "$(\n" +
                "$\tp = player();\n" +
                "$\t__assert_valid_for_motion();\n" +
                "$\t__prepare_path_if_needed();\n" +
                "$\tloop( length(global_points)-1,\n" +
                "$\t\tsegment = _;\n" +
                "$\t\tstart = element(element(global_points, segment),1);\n" +
                "$\t\tend = element(element(global_points, segment+1),1);\n" +
                "$\t\tloop(end-start,\n" +
                "$\t\t\tv = __get_path_at(segment, start, _);\n" +
                "$\t\t\tmodify(p, 'pos', slice(v,0,3));\n" +
                "$\t\t\tmodify(p, 'pitch', element(v,3));\n" +
                "$\t\t\tmodify(p, 'yaw', element(v,4));\n" +
                "$\t\t\tgame_tick(1000/fps)\n" +
                "$\t\t)\n" +
                "$\t);\n" +
                "$\tgame_tick(1000);\n" +
                "$\t'Done!'\n" +
                "$);\n" +
                "$\n" +
                "$ _show_path_tick(particle_type, total) -> (\n" +
                "$\t__assert_valid_for_motion();\n" +
                "$\t__prepare_path_if_needed();\n" +
                "$\tloop(total,\n" +
                "$\t\tsegment = floor(rand(length(global_points)-1));\n" +
                "$\t\tstart = element(element(global_points, segment),1);\n" +
                "$\t\tend = element(element(global_points, segment+1),1);\n" +
                "$\t\tindex = floor(rand(end-start));\n" +
                "$\t\tl(x, y, z) = slice(__get_path_at(segment, start, index), 0, 3);\n" +
                "$\t\tparticle(particle_type, x, y, z, 1, 0, 0)\n" +
                "$\t);\n" +
                "$\tnull\n" +
                "$);\n" +
                "$\n" +
                "$ __prepare_path_if_needed_generic() ->\n" +
                "$(\n" +
                "$\tif(!global_path_precalculated, __invalidate_points_cache())\n" +
                ");\n" +
                "\n" +
                "__find_position_for_linear(segment, point) ->\n" +
                "(\n" +
                "$\tl(va, start, mode_a) = element(global_points,segment);\n" +
                "$\tl(vb, end, mode_b)   = element(global_points,segment+1);\n" +
                "$\tsection = end-start;\n" +
                "$\tdt = point/section;\n" +
                "$\tdt*vb+(1-dt)*va\n" +
                ");\n" +
                "\n" +
                "\n" +
                "$'(1/sqrt(2*pi*d*d))*euler^(-((x-miu)^2)/(2*d*d)) ';\n" +
                "$ 'but we will be normalizing anyways, so who cares';\n" +
                "$ __norm_prob(x, miu, d) -> euler^(-((x-miu)^2)/(2*d*d));\n" +
                "$\n" +
                "$ __find_position_for_gauss(from_index, point) -> \n" +
                "$(\n" +
                "$\tdev = global_interpol_option;\n" +
                "$\tcomponents = l();\n" +
                "$\tpath_point = element(element(global_points, from_index),1);\n" +
                "$\t\n" +
                "$\ttry(\n" +
                "$\t\tfor(range(from_index+1, length(global_points)),\n" +
                "$\t\t\tl(v,ptime,mode) = element(global_points, _);\n" +
                "$\t\t\tdev = if (global_interpol_option > 0, global_interpol_option, \n" +
                "$\t\t\t\tdevs = l();\n" +
                "$\t\t\t\tif (_+1 < length(global_points), devs += element(element(global_points, _+1),1)-ptime);\n" +
                "$\t\t\t\tif (_-1 >= 0, devs += ptime-element(element(global_points, _-1),1));\n" +
                "$\t\t\t\t0.6*reduce(devs, _a+_, 0)/length(devs)\n" +
                "$\t\t\t);\n" +
                "$\t\t\timpact = __norm_prob(path_point+point, ptime, dev);\n" +
                "$\t\t\tif(rtotal && impact < 0.000001*rtotal, throw(null));\n" +
                "$\t\t\tcomponents += l(v, impact);\n" +
                "$\t\t\trtotal += impact\n" +
                "$\t\t)\n" +
                "$\t\t,null\n" +
                "$\t);\n" +
                "$\ttry(\n" +
                "$\t\tfor(range(from_index, -1, -1),\n" +
                "$\t\t\tl(v,ptime,mode) = element(global_points, _);\n" +
                "$\t\t\tdev = if (global_interpol_option > 0, global_interpol_option, \n" +
                "$\t\t\t\tdevs = l();\n" +
                "$\t\t\t\tif (_+1 < length(global_points), devs += element(element(global_points, _+1),1)-ptime);\n" +
                "$\t\t\t\tif (_-1 >= 0, devs += ptime-element(element(global_points, _-1),1));\n" +
                "$\t\t\t\t0.6*reduce(devs, _a+_, 0)/length(devs)\n" +
                "$\t\t\t);\n" +
                "$\t\t\timpact = __norm_prob(path_point+point, ptime, dev);\n" +
                "$\t\t\tif(ltotal && impact < 0.000001*ltotal, throw(null));\n" +
                "$\t\t\tcomponents += l(v, impact);\n" +
                "$\t\t\tltotal += impact\n" +
                "$\t\t)\n" +
                "$\t\t,null\n" +
                "$\t);\n" +
                "$\ttotal = rtotal+ltotal;\n" +
                "$\treduce(components, _a+element(_,0)*(element(_,1)/total), l(0,0,0,0,0))\n" +
                "$);\n" +
                "\n" +
                "\n" +
                "$ __prepare_path_if_needed_bungee(fps) ->\n" +
                "$(\n" +
                "$\texit('not ready!');\n" +
                "$\tif(!global_points, exit('Cannot show path that doesn\\'t exist'));\n" +
                "$\tif(length(global_points)<2, exit('Path not complete - add more points'));\n" +
                "$\tv = element(element(global_points, 0),0);\n" +
                "$\tmodify(p, 'pos', slice(v,0,3));\n" +
                "$\tmodify(p, 'pitch', element(v,3));\n" +
                "$\tmodify(p, 'yaw', element(v,4));\n" +
                "$\tcurrent_target = element(element(global_points, 1),0);\n" +
                "$\tcurrent_hook = 1;\n" +
                "$\tloop(length(global_points)-1, \n" +
                "$\t\tcurrent_base = _;\n" +
                "$\t\tl(va, start, mode_a) = element(global_points,_);\n" +
                "$\t\tl(vb, end, mode_b)   = element(global_points,_+1);\n" +
                "$\t\tsection = end-start;\n" +
                "$       loop(section,\n" +
                "$\t\t\tdt = _/section;\n" +
                "\n" +
                "$\t\t\tv = dt*vb+(1-dt)*va;\n" +
                "$\t\t\tmodify(p, 'pos', slice(v,0,3));\n" +
                "$\t\t\tmodify(p, 'pitch', element(v,3));\n" +
                "$\t\t\tmodify(p, 'yaw', element(v,4));\n" +
                "$\t\t\tgame_tick(1000/fps)\n" +
                "$\t\t)\n" +
                "$\t);\n" +
                "$\tgame_tick(1000);\n" +
                "$\t'Done!'\n" +
                "$)\n" +
                "$";
    }
}
