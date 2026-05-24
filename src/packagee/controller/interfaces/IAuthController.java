package packagee.controller.interfaces;

import packagee.controller.IController;
import packagee.controller.Response;

public interface IAuthController extends IController {

    Response login(String username, String password);
}
