#include <AL/al.h>
#include <AL/alc.h>
#include <SDL3/SDL.h>
#include <SDL3/SDL_main.h>

int main(int argc, char **argv)
{
    (void)argc;
    (void)argv;

    if (!SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_GAMEPAD)) {
        SDL_Log("SDL_Init failed: %s", SDL_GetError());
        return 1;
    }

    SDL_Window *window = SDL_CreateWindow(
        "Cinderhell native smoke test",
        1280,
        720,
        SDL_WINDOW_FULLSCREEN);
    if (window == NULL) {
        SDL_Log("SDL_CreateWindow failed: %s", SDL_GetError());
        SDL_Quit();
        return 2;
    }

    SDL_Renderer *renderer = SDL_CreateRenderer(window, NULL);
    if (renderer == NULL) {
        SDL_Log("SDL_CreateRenderer failed: %s", SDL_GetError());
        SDL_DestroyWindow(window);
        SDL_Quit();
        return 3;
    }

    SDL_Log("Cinderhell SDL3 smoke runtime started");

    ALCdevice *audio_device = alcOpenDevice(NULL);
    if (audio_device == NULL) {
        SDL_Log("OpenAL device initialization failed");
    }

    ALCcontext *audio_context =
        audio_device == NULL ? NULL : alcCreateContext(audio_device, NULL);
    if (audio_context != NULL && alcMakeContextCurrent(audio_context)) {
        SDL_Log("Cinderhell OpenAL smoke device initialized: %s",
                alGetString(AL_RENDERER));
    } else if (audio_device != NULL) {
        SDL_Log("OpenAL context initialization failed");
    }

    bool running = true;
    while (running) {
        SDL_Event event;
        while (SDL_PollEvent(&event)) {
            if (event.type == SDL_EVENT_QUIT) {
                running = false;
            } else if (event.type == SDL_EVENT_KEY_DOWN &&
                       event.key.key == SDLK_AC_BACK) {
                running = false;
            } else if (event.type == SDL_EVENT_GAMEPAD_BUTTON_DOWN &&
                       event.gbutton.button == SDL_GAMEPAD_BUTTON_EAST) {
                running = false;
            }
        }

        SDL_SetRenderDrawColor(renderer, 22, 11, 5, 255);
        SDL_RenderClear(renderer);
        SDL_SetRenderDrawColor(renderer, 255, 176, 0, 255);
        SDL_FRect ember = {440.0f, 160.0f, 400.0f, 400.0f};
        SDL_RenderFillRect(renderer, &ember);
        SDL_RenderPresent(renderer);
        SDL_Delay(8);
    }

    SDL_Log("Cinderhell SDL3 smoke runtime returning normally");
    if (audio_context != NULL) {
        alcMakeContextCurrent(NULL);
        alcDestroyContext(audio_context);
    }
    if (audio_device != NULL) {
        alcCloseDevice(audio_device);
    }
    SDL_DestroyRenderer(renderer);
    SDL_DestroyWindow(window);
    SDL_Quit();
    return 0;
}
