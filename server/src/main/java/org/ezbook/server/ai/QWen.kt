/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.ezbook.server.ai

import org.ezbook.server.constant.AIModel

class QWen : BaseAi() {

    override var api: String
        get() = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"
        set(value) {}
    override var model: String
        get() = "qwen-turbo"
        set(value) {}
    override var name: String
        get() = AIModel.QWen.name
        set(value) {}
    override var createKeyUri: String
        get() = "https://help.aliyun.com/zh/model-studio/developer-reference/get-api-key"
        set(value) {}
}